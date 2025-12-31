package ma.emsi.notificationservice.controllers;

import ma.emsi.notificationservice.dtos.NotificationStatsDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.entities.NotificationDLQ;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import ma.emsi.notificationservice.repositories.NotificationDLQRepository;
import ma.emsi.notificationservice.repositories.NotificationRepository;
import ma.emsi.notificationservice.services.NotificationHistoryService;
import ma.emsi.notificationservice.services.NotificationProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationController.
 * Tests REST API endpoints with mocked services.
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    
    @Mock
    private NotificationHistoryService notificationHistoryService;
    
    @Mock
    private NotificationProcessingService notificationProcessingService;
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private NotificationDLQRepository notificationDLQRepository;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private NotificationController notificationController;
    
    private Notification testNotification;
    private Page<Notification> testPage;
    
    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .type(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR)
                .destinataire("test@example.com")
                .sujet("Test Subject")
                .messageTexte("Test message")
                .statut(StatutNotification.SENT)
                .nombreTentatives(0)
                .dateCreation(LocalDateTime.now())
                .build();
        
        List<Notification> notifications = List.of(testNotification);
        testPage = new PageImpl<>(notifications, PageRequest.of(0, 20), 1);
    }
    
    @Test
    void testGetAllNotifications_Success() {
        // Arrange
        when(notificationRepository.findAll(any(Pageable.class))).thenReturn(testPage);
        
        // Act
        ResponseEntity<Page<Notification>> response = notificationController.getAllNotifications(0, 20, "dateCreation", "DESC");
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(notificationRepository, times(1)).findAll(any(Pageable.class));
    }
    
    @Test
    void testGetNotificationById_Success() {
        // Arrange
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(authentication.getPrincipal()).thenReturn("test@example.com");
        
        // Act
        ResponseEntity<Notification> response = notificationController.getNotificationById(1L, authentication);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("test@example.com", response.getBody().getDestinataire());
        verify(notificationRepository, times(1)).findById(1L);
    }
    
    @Test
    void testGetNotificationById_NotFound() {
        // Arrange
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            notificationController.getNotificationById(999L, authentication);
        });
        verify(notificationRepository, times(1)).findById(999L);
    }
    
    @Test
    void testGetNotificationsByUser_Success() {
        // Arrange
        String email = "test@example.com";
        when(notificationHistoryService.getNotificationsByUser(eq(email), any(Pageable.class)))
                .thenReturn(testPage);
        
        // Act
        ResponseEntity<Page<Notification>> response = notificationController.getNotificationsByUser(email, 0, 20);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(notificationHistoryService, times(1)).getNotificationsByUser(eq(email), any(Pageable.class));
    }
    
    @Test
    void testGetNotificationsByStatus_Success() {
        // Arrange
        StatutNotification status = StatutNotification.SENT;
        when(notificationHistoryService.getNotificationsByStatus(eq(status), any(Pageable.class)))
                .thenReturn(testPage);
        
        // Act
        ResponseEntity<Page<Notification>> response = notificationController.getNotificationsByStatus(status, 0, 20);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(notificationHistoryService, times(1)).getNotificationsByStatus(eq(status), any(Pageable.class));
    }
    
    @Test
    void testGetNotificationStats_Success() {
        // Arrange
        NotificationStatsDTO stats = NotificationStatsDTO.builder()
                .total(100L)
                .sent(80L)
                .failed(10L)
                .pending(5L)
                .retrying(5L)
                .build();
        stats.calculateSuccessRate();
        
        when(notificationHistoryService.getNotificationsStats()).thenReturn(stats);
        
        // Act
        ResponseEntity<NotificationStatsDTO> response = notificationController.getNotificationStats();
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().getTotal());
        assertEquals(80L, response.getBody().getSent());
        assertEquals(10L, response.getBody().getFailed());
        verify(notificationHistoryService, times(1)).getNotificationsStats();
    }
    
    @Test
    void testRetryNotification_Success() {
        // Arrange
        Notification failedNotification = Notification.builder()
                .id(1L)
                .statut(StatutNotification.PENDING)
                .build();
        
        when(notificationHistoryService.retryFailedNotification(1L)).thenReturn(failedNotification);
        
        // Act
        ResponseEntity<Map<String, Object>> response = notificationController.retryNotification(1L);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(1L, response.getBody().get("notificationId"));
        verify(notificationHistoryService, times(1)).retryFailedNotification(1L);
    }
    
    @Test
    void testRetryNotification_NotFound() {
        // Arrange
        when(notificationHistoryService.retryFailedNotification(999L))
                .thenThrow(new IllegalArgumentException("Notification not found"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = notificationController.retryNotification(999L);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        verify(notificationHistoryService, times(1)).retryFailedNotification(999L);
    }
    
    @Test
    void testRetryNotification_InvalidState() {
        // Arrange
        when(notificationHistoryService.retryFailedNotification(1L))
                .thenThrow(new IllegalStateException("Can only retry notifications with FAILED status"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = notificationController.retryNotification(1L);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        verify(notificationHistoryService, times(1)).retryFailedNotification(1L);
    }
    
    @Test
    void testGetFailedNotifications_Success() {
        // Arrange
        when(notificationHistoryService.getNotificationsByStatus(eq(StatutNotification.FAILED), any(Pageable.class)))
                .thenReturn(testPage);
        
        // Act
        ResponseEntity<Page<Notification>> response = notificationController.getFailedNotifications(0, 20);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(notificationHistoryService, times(1))
                .getNotificationsByStatus(eq(StatutNotification.FAILED), any(Pageable.class));
    }
    
    @Test
    void testSearchNotifications_Success() {
        // Arrange
        String destinataire = "test@example.com";
        TypeNotification type = TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR;
        StatutNotification status = StatutNotification.SENT;
        LocalDateTime dateDebut = LocalDateTime.now().minusDays(7);
        LocalDateTime dateFin = LocalDateTime.now();
        
        when(notificationRepository.searchNotifications(
                eq(destinataire), eq(type), eq(status), eq(dateDebut), eq(dateFin), any(Pageable.class)))
                .thenReturn(testPage);
        
        // Act
        ResponseEntity<Page<Notification>> response = notificationController.searchNotifications(
                destinataire, type, status, dateDebut, dateFin, 0, 20);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(notificationRepository, times(1)).searchNotifications(
                eq(destinataire), eq(type), eq(status), eq(dateDebut), eq(dateFin), any(Pageable.class));
    }
    
    @Test
    void testSearchNotifications_WithNullFilters() {
        // Arrange
        when(notificationRepository.searchNotifications(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(testPage);
        
        // Act
        ResponseEntity<Page<Notification>> response = notificationController.searchNotifications(
                null, null, null, null, null, 0, 20);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationRepository, times(1)).searchNotifications(
                isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }
    
    @Test
    void testRetryAllDLQMessages_Success() {
        // Arrange
        NotificationDLQ dlqEntry1 = new NotificationDLQ();
        dlqEntry1.setId(1L);
        dlqEntry1.setNotificationId(10L);
        dlqEntry1.setRetraite(false);
        
        NotificationDLQ dlqEntry2 = new NotificationDLQ();
        dlqEntry2.setId(2L);
        dlqEntry2.setNotificationId(20L);
        dlqEntry2.setRetraite(false);
        
        List<NotificationDLQ> dlqEntries = List.of(dlqEntry1, dlqEntry2);
        
        when(notificationDLQRepository.findByRetraiteOrderByDateAjoutDlqAsc(false))
                .thenReturn(dlqEntries);
        when(notificationHistoryService.retryFailedNotification(anyLong()))
                .thenReturn(testNotification);
        
        // Act
        ResponseEntity<Map<String, Object>> response = notificationController.retryAllDLQMessages();
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(2, response.getBody().get("totalProcessed"));
        assertEquals(2, response.getBody().get("successCount"));
        assertEquals(0, response.getBody().get("failureCount"));
        verify(notificationDLQRepository, times(1)).findByRetraiteOrderByDateAjoutDlqAsc(false);
        verify(notificationHistoryService, times(2)).retryFailedNotification(anyLong());
    }
    
    @Test
    void testRetryAllDLQMessages_PartialSuccess() {
        // Arrange
        NotificationDLQ dlqEntry1 = new NotificationDLQ();
        dlqEntry1.setId(1L);
        dlqEntry1.setNotificationId(10L);
        dlqEntry1.setRetraite(false);
        
        NotificationDLQ dlqEntry2 = new NotificationDLQ();
        dlqEntry2.setId(2L);
        dlqEntry2.setNotificationId(20L);
        dlqEntry2.setRetraite(false);
        
        List<NotificationDLQ> dlqEntries = List.of(dlqEntry1, dlqEntry2);
        
        when(notificationDLQRepository.findByRetraiteOrderByDateAjoutDlqAsc(false))
                .thenReturn(dlqEntries);
        when(notificationHistoryService.retryFailedNotification(10L))
                .thenReturn(testNotification);
        when(notificationHistoryService.retryFailedNotification(20L))
                .thenThrow(new RuntimeException("Retry failed"));
        
        // Act
        ResponseEntity<Map<String, Object>> response = notificationController.retryAllDLQMessages();
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(2, response.getBody().get("totalProcessed"));
        assertEquals(1, response.getBody().get("successCount"));
        assertEquals(1, response.getBody().get("failureCount"));
    }
    
    @Test
    void testRetryAllDLQMessages_EmptyDLQ() {
        // Arrange
        when(notificationDLQRepository.findByRetraiteOrderByDateAjoutDlqAsc(false))
                .thenReturn(new ArrayList<>());
        
        // Act
        ResponseEntity<Map<String, Object>> response = notificationController.retryAllDLQMessages();
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(0, response.getBody().get("totalProcessed"));
        assertEquals(0, response.getBody().get("successCount"));
        assertEquals(0, response.getBody().get("failureCount"));
    }
    
    @Test
    void testIsOwner_UserIsOwner() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn("test@example.com");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        
        // Act
        boolean result = notificationController.isOwner(1L, authentication);
        
        // Assert
        assertTrue(result);
        verify(notificationRepository, times(1)).findById(1L);
    }
    
    @Test
    void testIsOwner_UserIsNotOwner() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn("other@example.com");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        
        // Act
        boolean result = notificationController.isOwner(1L, authentication);
        
        // Assert
        assertFalse(result);
        verify(notificationRepository, times(1)).findById(1L);
    }
    
    @Test
    void testIsOwner_NotificationNotFound() {
        // Arrange
        when(authentication.getPrincipal()).thenReturn("test@example.com");
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        boolean result = notificationController.isOwner(999L, authentication);
        
        // Assert
        assertFalse(result);
        verify(notificationRepository, times(1)).findById(999L);
    }
    
    @Test
    void testIsOwner_NullAuthentication() {
        // Act
        boolean result = notificationController.isOwner(1L, null);
        
        // Assert
        assertFalse(result);
        verify(notificationRepository, never()).findById(anyLong());
    }
}
