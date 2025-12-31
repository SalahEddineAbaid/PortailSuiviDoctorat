package ma.emsi.notificationservice.consumers;

import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import ma.emsi.notificationservice.services.NotificationProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationConsumer.
 * Tests Kafka message consumption and delegation to NotificationProcessingService.
 */
@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {
    
    @Mock
    private NotificationProcessingService notificationProcessingService;
    
    @InjectMocks
    private NotificationConsumer notificationConsumer;
    
    private NotificationDTO testNotificationDTO;
    
    @BeforeEach
    void setUp() {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("nomDoctorant", "Ahmed Bennani");
        donnees.put("nomDirecteur", "Dr. Alami");
        
        testNotificationDTO = NotificationDTO.builder()
            .type(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR)
            .destinataire("directeur@emsi.ma")
            .sujet("Nouvelle inscription soumise")
            .messageTexte("Une nouvelle inscription a été soumise")
            .priorite(PrioriteNotification.NORMALE)
            .donnees(donnees)
            .build();
    }
    
    @Test
    void testConsumeNotification_Success() {
        // Arrange
        int partition = 0;
        long offset = 123L;
        
        // Act
        notificationConsumer.consumeNotification(testNotificationDTO, partition, offset);
        
        // Assert
        verify(notificationProcessingService, times(1)).processNotification(testNotificationDTO);
    }
    
    @Test
    void testConsumeNotification_ProcessingServiceThrowsException() {
        // Arrange
        int partition = 0;
        long offset = 123L;
        doThrow(new RuntimeException("Processing error"))
            .when(notificationProcessingService)
            .processNotification(any(NotificationDTO.class));
        
        // Act - should not throw exception, just log it
        notificationConsumer.consumeNotification(testNotificationDTO, partition, offset);
        
        // Assert - verify processing was attempted
        verify(notificationProcessingService, times(1)).processNotification(testNotificationDTO);
    }
    
    @Test
    void testConsumeNotification_WithDifferentNotificationType() {
        // Arrange
        NotificationDTO inscriptionValideeDTO = NotificationDTO.builder()
            .type(TypeNotification.INSCRIPTION_VALIDEE_DIRECTEUR_DOCTORANT)
            .destinataire("doctorant@emsi.ma")
            .sujet("Inscription validée")
            .messageTexte("Votre inscription a été validée")
            .priorite(PrioriteNotification.HAUTE)
            .build();
        
        int partition = 1;
        long offset = 456L;
        
        // Act
        notificationConsumer.consumeNotification(inscriptionValideeDTO, partition, offset);
        
        // Assert
        verify(notificationProcessingService, times(1)).processNotification(inscriptionValideeDTO);
    }
    
    @Test
    void testConsumeNotification_WithNullDonnees() {
        // Arrange
        NotificationDTO notificationWithNullDonnees = NotificationDTO.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("user@emsi.ma")
            .sujet("Test notification")
            .messageTexte("Test message")
            .priorite(PrioriteNotification.NORMALE)
            .donnees(null)
            .build();
        
        int partition = 0;
        long offset = 789L;
        
        // Act
        notificationConsumer.consumeNotification(notificationWithNullDonnees, partition, offset);
        
        // Assert
        verify(notificationProcessingService, times(1)).processNotification(notificationWithNullDonnees);
    }
}
