package ma.emsi.notificationservice.services;

import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationProcessingService.
 * Tests validation, template selection, and orchestration logic.
 */
@ExtendWith(MockitoExtension.class)
class NotificationProcessingServiceTest {
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private EmailTemplateService emailTemplateService;
    
    @Mock
    private NotificationHistoryService notificationHistoryService;
    
    @Mock
    private KafkaTemplate<String, NotificationDTO> kafkaTemplate;
    
    @InjectMocks
    private NotificationProcessingService notificationProcessingService;
    
    private NotificationDTO validNotificationDTO;
    private Notification mockNotification;
    
    @BeforeEach
    void setUp() {
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("nomDoctorant", "Ahmed Benali");
        donnees.put("nomDirecteur", "Dr. Fatima Zahra");
        
        validNotificationDTO = NotificationDTO.builder()
                .type(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR)
                .destinataire("directeur@emsi.ma")
                .sujet("Nouvelle inscription soumise")
                .messageTexte("Une nouvelle inscription a été soumise")
                .priorite(PrioriteNotification.NORMALE)
                .donnees(donnees)
                .build();
        
        mockNotification = Notification.builder()
                .id(1L)
                .type(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR)
                .destinataire("directeur@emsi.ma")
                .sujet("Nouvelle inscription soumise")
                .statut(StatutNotification.PENDING)
                .nombreTentatives(0)
                .build();
    }
    
    @Test
    void testValidateNotification_ValidEmail_ShouldPass() {
        // Act & Assert
        assertDoesNotThrow(() -> notificationProcessingService.validateNotification(validNotificationDTO));
    }
    
    @Test
    void testValidateNotification_InvalidEmail_ShouldThrowException() {
        // Arrange
        validNotificationDTO.setDestinataire("invalid-email");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> notificationProcessingService.validateNotification(validNotificationDTO)
        );
        
        assertTrue(exception.getMessage().contains("Invalid email format"));
    }
    
    @Test
    void testValidateNotification_NullEmail_ShouldThrowException() {
        // Arrange
        validNotificationDTO.setDestinataire(null);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> notificationProcessingService.validateNotification(validNotificationDTO)
        );
        
        assertTrue(exception.getMessage().contains("email is required"));
    }
    
    @Test
    void testValidateNotification_EmptyEmail_ShouldThrowException() {
        // Arrange
        validNotificationDTO.setDestinataire("   ");
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> notificationProcessingService.validateNotification(validNotificationDTO)
        );
        
        assertTrue(exception.getMessage().contains("email is required"));
    }
    
    @Test
    void testValidateNotification_NullType_ShouldThrowException() {
        // Arrange
        validNotificationDTO.setType(null);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> notificationProcessingService.validateNotification(validNotificationDTO)
        );
        
        assertTrue(exception.getMessage().contains("type is required"));
    }
    
    @Test
    void testValidateNotification_NullSubject_ShouldThrowException() {
        // Arrange
        validNotificationDTO.setSujet(null);
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> notificationProcessingService.validateNotification(validNotificationDTO)
        );
        
        assertTrue(exception.getMessage().contains("Subject is required"));
    }
    
    @Test
    void testSelectTemplate_ValidType_ShouldReturnTemplateName() {
        // Arrange
        when(emailTemplateService.getTemplateForNotificationType(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR))
            .thenReturn("template_inscription_soumise_directeur.html");
        
        // Act
        String templateName = notificationProcessingService.selectTemplate(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR);
        
        // Assert
        assertEquals("template_inscription_soumise_directeur.html", templateName);
        verify(emailTemplateService).getTemplateForNotificationType(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR);
    }
    
    @Test
    void testPrepareTemplateVariables_WithDonnees_ShouldIncludeAllVariables() {
        // Act
        Map<String, Object> variables = notificationProcessingService.prepareTemplateVariables(validNotificationDTO);
        
        // Assert
        assertNotNull(variables);
        assertEquals("Ahmed Benali", variables.get("nomDoctorant"));
        assertEquals("Dr. Fatima Zahra", variables.get("nomDirecteur"));
        assertEquals("directeur@emsi.ma", variables.get("destinataire"));
        assertEquals("Nouvelle inscription soumise", variables.get("sujet"));
    }
    
    @Test
    void testPrepareTemplateVariables_NullDonnees_ShouldCreateMapWithStandardVariables() {
        // Arrange
        validNotificationDTO.setDonnees(null);
        
        // Act
        Map<String, Object> variables = notificationProcessingService.prepareTemplateVariables(validNotificationDTO);
        
        // Assert
        assertNotNull(variables);
        assertEquals("directeur@emsi.ma", variables.get("destinataire"));
        assertEquals("Nouvelle inscription soumise", variables.get("sujet"));
    }
    
    @Test
    void testHandleSuccess_ShouldUpdateStatusToSent() {
        // Act
        notificationProcessingService.handleSuccess(1L);
        
        // Assert
        verify(notificationHistoryService).updateNotificationStatus(
            eq(1L),
            eq(StatutNotification.SENT),
            isNull()
        );
    }
    
    @Test
    void testHandleFailure_WithRetry_ShouldUpdateStatusAndSendToDLQ() {
        // Act
        notificationProcessingService.handleFailure(1L, "Email send failed", true);
        
        // Assert
        verify(notificationHistoryService).updateNotificationStatus(
            eq(1L),
            eq(StatutNotification.FAILED),
            eq("Email send failed")
        );
        verify(kafkaTemplate).send(eq("notifications-dlq"), anyString(), any(NotificationDTO.class));
    }
    
    @Test
    void testHandleFailure_WithoutRetry_ShouldOnlyUpdateStatus() {
        // Act
        notificationProcessingService.handleFailure(1L, "Validation error", false);
        
        // Assert
        verify(notificationHistoryService).updateNotificationStatus(
            eq(1L),
            eq(StatutNotification.FAILED),
            eq("Validation error")
        );
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(NotificationDTO.class));
    }
    
    @Test
    void testSendToDLQ_ShouldSendMessageToKafka() {
        // Act
        notificationProcessingService.sendToDLQ(1L, "Test error");
        
        // Assert
        verify(kafkaTemplate).send(
            eq("notifications-dlq"),
            eq("1"),
            any(NotificationDTO.class)
        );
    }
    
    @Test
    void testProcessNotification_ValidNotification_ShouldCompleteSuccessfully() throws Exception {
        // Arrange
        when(notificationHistoryService.saveNotification(any(NotificationDTO.class)))
            .thenReturn(mockNotification);
        when(emailTemplateService.processTemplate(any(TypeNotification.class), anyMap()))
            .thenReturn("<html>Test email</html>");
        when(emailService.sendEmail(anyString(), anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // Act
        notificationProcessingService.processNotification(validNotificationDTO);
        
        // Assert
        verify(notificationHistoryService).saveNotification(validNotificationDTO);
        verify(emailTemplateService).processTemplate(any(TypeNotification.class), anyMap());
        verify(emailService).sendEmail(
            eq("directeur@emsi.ma"),
            eq("Nouvelle inscription soumise"),
            anyString()
        );
        verify(notificationHistoryService).updateNotificationStatus(
            eq(1L),
            eq(StatutNotification.SENT),
            isNull()
        );
    }
    
    @Test
    void testProcessNotification_InvalidEmail_ShouldHandleFailureWithoutRetry() {
        // Arrange
        validNotificationDTO.setDestinataire("invalid-email");
        when(notificationHistoryService.saveNotification(any(NotificationDTO.class)))
            .thenReturn(mockNotification);
        
        // Act
        notificationProcessingService.processNotification(validNotificationDTO);
        
        // Assert
        verify(notificationHistoryService).saveNotification(validNotificationDTO);
        verify(notificationHistoryService).updateNotificationStatus(
            eq(1L),
            eq(StatutNotification.FAILED),
            contains("Invalid email format")
        );
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any(NotificationDTO.class));
    }
}
