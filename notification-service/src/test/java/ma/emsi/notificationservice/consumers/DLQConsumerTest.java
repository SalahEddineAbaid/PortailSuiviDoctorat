package ma.emsi.notificationservice.consumers;

import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.entities.NotificationDLQ;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import ma.emsi.notificationservice.repositories.NotificationDLQRepository;
import ma.emsi.notificationservice.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DLQConsumer.
 * Tests DLQ message consumption and persistence.
 * 
 * Requirements: 11.2, 11.3
 */
@ExtendWith(MockitoExtension.class)
class DLQConsumerTest {
    
    @Mock
    private NotificationDLQRepository notificationDLQRepository;
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @InjectMocks
    private DLQConsumer dlqConsumer;
    
    private NotificationDTO testDLQNotificationDTO;
    private Notification testOriginalNotification;
    
    @BeforeEach
    void setUp() {
        // Create a DLQ notification DTO (as sent by NotificationProcessingService)
        testDLQNotificationDTO = NotificationDTO.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("dlq@system.internal")
            .sujet("DLQ - Notification 123")
            .messageTexte("Failed notification ID: 123. Error: SMTP connection timeout")
            .priorite(PrioriteNotification.NORMALE)
            .build();
        
        // Create the original notification that failed
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("nomDoctorant", "Ahmed Bennani");
        donnees.put("nomDirecteur", "Dr. Alami");
        
        testOriginalNotification = Notification.builder()
            .id(123L)
            .type(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR)
            .destinataire("directeur@emsi.ma")
            .sujet("Nouvelle inscription soumise")
            .messageTexte("Une nouvelle inscription a été soumise")
            .messageHtml("<html>...</html>")
            .statut(StatutNotification.FAILED)
            .priorite(PrioriteNotification.NORMALE)
            .donnees("{\"nomDoctorant\":\"Ahmed Bennani\",\"nomDirecteur\":\"Dr. Alami\"}")
            .nombreTentatives(3)
            .erreurMessage("SMTP connection timeout")
            .dateCreation(LocalDateTime.now())
            .build();
    }
    
    @Test
    void testConsumeDLQNotification_Success() {
        // Arrange
        int partition = 0;
        long offset = 456L;
        
        when(notificationRepository.findById(123L))
            .thenReturn(Optional.of(testOriginalNotification));
        
        when(notificationDLQRepository.save(any(NotificationDLQ.class)))
            .thenAnswer(invocation -> {
                NotificationDLQ dlq = invocation.getArgument(0);
                dlq.setId(1L);
                return dlq;
            });
        
        // Act
        dlqConsumer.consumeDLQNotification(testDLQNotificationDTO, partition, offset);
        
        // Assert - verify DLQ entry was saved
        ArgumentCaptor<NotificationDLQ> dlqCaptor = ArgumentCaptor.forClass(NotificationDLQ.class);
        verify(notificationDLQRepository, times(1)).save(dlqCaptor.capture());
        
        NotificationDLQ savedDLQ = dlqCaptor.getValue();
        assertEquals(123L, savedDLQ.getNotificationId());
        assertEquals(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR, savedDLQ.getType());
        assertEquals("directeur@emsi.ma", savedDLQ.getDestinataire());
        assertEquals("Nouvelle inscription soumise", savedDLQ.getSujet());
        assertEquals("SMTP connection timeout", savedDLQ.getErreurMessage());
        assertEquals(3, savedDLQ.getNombreTentatives());
        assertEquals(partition, savedDLQ.getKafkaPartition());
        assertEquals(offset, savedDLQ.getKafkaOffset());
        assertFalse(savedDLQ.getRetraite());
    }
    
    @Test
    void testConsumeDLQNotification_OriginalNotificationNotFound() {
        // Arrange
        int partition = 0;
        long offset = 456L;
        
        when(notificationRepository.findById(123L))
            .thenReturn(Optional.empty());
        
        // Act
        dlqConsumer.consumeDLQNotification(testDLQNotificationDTO, partition, offset);
        
        // Assert - verify DLQ entry was NOT saved
        verify(notificationDLQRepository, never()).save(any(NotificationDLQ.class));
    }
    
    @Test
    void testConsumeDLQNotification_InvalidMessageFormat() {
        // Arrange
        NotificationDTO invalidDLQMessage = NotificationDTO.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("dlq@system.internal")
            .sujet("DLQ - Invalid")
            .messageTexte("This message does not contain a notification ID")
            .build();
        
        int partition = 0;
        long offset = 456L;
        
        // Act
        dlqConsumer.consumeDLQNotification(invalidDLQMessage, partition, offset);
        
        // Assert - verify DLQ entry was NOT saved
        verify(notificationRepository, never()).findById(any());
        verify(notificationDLQRepository, never()).save(any(NotificationDLQ.class));
    }
    
    @Test
    void testConsumeDLQNotification_RepositoryThrowsException() {
        // Arrange
        int partition = 0;
        long offset = 456L;
        
        when(notificationRepository.findById(123L))
            .thenReturn(Optional.of(testOriginalNotification));
        
        when(notificationDLQRepository.save(any(NotificationDLQ.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        // Act - should not throw exception, just log it
        dlqConsumer.consumeDLQNotification(testDLQNotificationDTO, partition, offset);
        
        // Assert - verify save was attempted
        verify(notificationDLQRepository, times(1)).save(any(NotificationDLQ.class));
    }
    
    @Test
    void testConsumeDLQNotification_WithDifferentNotificationType() {
        // Arrange
        NotificationDTO dlqMessage = NotificationDTO.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("dlq@system.internal")
            .sujet("DLQ - Notification 456")
            .messageTexte("Failed notification ID: 456. Error: Invalid email format")
            .build();
        
        Notification failedNotification = Notification.builder()
            .id(456L)
            .type(TypeNotification.JURY_PROPOSE_ADMIN)
            .destinataire("admin@emsi.ma")
            .sujet("Jury proposé")
            .messageTexte("Un jury a été proposé")
            .statut(StatutNotification.FAILED)
            .priorite(PrioriteNotification.HAUTE)
            .nombreTentatives(1)
            .erreurMessage("Invalid email format")
            .dateCreation(LocalDateTime.now())
            .build();
        
        int partition = 1;
        long offset = 789L;
        
        when(notificationRepository.findById(456L))
            .thenReturn(Optional.of(failedNotification));
        
        when(notificationDLQRepository.save(any(NotificationDLQ.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        dlqConsumer.consumeDLQNotification(dlqMessage, partition, offset);
        
        // Assert
        ArgumentCaptor<NotificationDLQ> dlqCaptor = ArgumentCaptor.forClass(NotificationDLQ.class);
        verify(notificationDLQRepository, times(1)).save(dlqCaptor.capture());
        
        NotificationDLQ savedDLQ = dlqCaptor.getValue();
        assertEquals(456L, savedDLQ.getNotificationId());
        assertEquals(TypeNotification.JURY_PROPOSE_ADMIN, savedDLQ.getType());
        assertEquals("admin@emsi.ma", savedDLQ.getDestinataire());
    }
}
