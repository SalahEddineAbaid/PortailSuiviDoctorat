package ma.emsi.notificationservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.dtos.NotificationStatsDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import ma.emsi.notificationservice.repositories.NotificationRepository;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationHistoryService.
 * Tests notification persistence, status updates, and statistics.
 */
@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {
    
    @Mock
    private NotificationRepository notificationRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @InjectMocks
    private NotificationHistoryService notificationHistoryService;
    
    private NotificationDTO testNotificationDTO;
    private Notification testNotification;
    
    @BeforeEach
    void setUp() {
        // Setup test data
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("nomDoctorant", "John Doe");
        donnees.put("sujetThese", "AI Research");
        
        testNotificationDTO = NotificationDTO.builder()
                .type(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR)
                .destinataire("directeur@example.com")
                .sujet("Nouvelle inscription soumise")
                .messageTexte("Une nouvelle inscription a été soumise")
                .priorite(PrioriteNotification.NORMALE)
                .donnees(donnees)
                .build();
        
        testNotification = Notification.builder()
                .id(1L)
                .type(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR)
                .destinataire("directeur@example.com")
                .sujet("Nouvelle inscription soumise")
                .messageTexte("Une nouvelle inscription a été soumise")
                .statut(StatutNotification.PENDING)
                .priorite(PrioriteNotification.NORMALE)
                .donnees("{\"nomDoctorant\":\"John Doe\",\"sujetThese\":\"AI Research\"}")
                .nombreTentatives(0)
                .dateCreation(LocalDateTime.now())
                .build();
    }
    
    @Test
    void testSaveNotification_success() throws Exception {
        // Given
        String donneesJson = "{\"nomDoctorant\":\"John Doe\",\"sujetThese\":\"AI Research\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(donneesJson);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        Notification result = notificationHistoryService.saveNotification(testNotificationDTO);
        
        // Then
        assertNotNull(result);
        assertEquals(StatutNotification.PENDING, result.getStatut());
        assertEquals(0, result.getNombreTentatives());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
    
    @Test
    void testSaveNotification_withNullDonnees() throws Exception {
        // Given
        testNotificationDTO.setDonnees(null);
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        Notification result = notificationHistoryService.saveNotification(testNotificationDTO);
        
        // Then
        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(objectMapper, never()).writeValueAsString(any());
    }
    
    @Test
    void testSaveNotification_withEmptyDonnees() throws Exception {
        // Given
        testNotificationDTO.setDonnees(new HashMap<>());
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        Notification result = notificationHistoryService.saveNotification(testNotificationDTO);
        
        // Then
        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(objectMapper, never()).writeValueAsString(any());
    }
    
    @Test
    void testUpdateNotificationStatus_toSent() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        notificationHistoryService.updateNotificationStatus(1L, StatutNotification.SENT, null);
        
        // Then
        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
    
    @Test
    void testUpdateNotificationStatus_toFailed() {
        // Given
        String errorMessage = "SMTP connection failed";
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        notificationHistoryService.updateNotificationStatus(1L, StatutNotification.FAILED, errorMessage);
        
        // Then
        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
    
    @Test
    void testUpdateNotificationStatus_toRetrying() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        notificationHistoryService.updateNotificationStatus(1L, StatutNotification.RETRYING, null);
        
        // Then
        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
    
    @Test
    void testUpdateNotificationStatus_notFound() {
        // Given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            notificationHistoryService.updateNotificationStatus(999L, StatutNotification.SENT, null);
        });
    }
    
    @Test
    void testIncrementRetryCount_success() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        notificationHistoryService.incrementRetryCount(1L);
        
        // Then
        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
    
    @Test
    void testIncrementRetryCount_notFound() {
        // Given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            notificationHistoryService.incrementRetryCount(999L);
        });
    }
    
    @Test
    void testGetNotificationsByUser() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findByDestinataire("directeur@example.com", pageable))
                .thenReturn(page);
        
        // When
        Page<Notification> result = notificationHistoryService.getNotificationsByUser(
                "directeur@example.com", pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository, times(1)).findByDestinataire("directeur@example.com", pageable);
    }
    
    @Test
    void testGetNotificationsByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findByStatut(StatutNotification.PENDING, pageable))
                .thenReturn(page);
        
        // When
        Page<Notification> result = notificationHistoryService.getNotificationsByStatus(
                StatutNotification.PENDING, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository, times(1)).findByStatut(StatutNotification.PENDING, pageable);
    }
    
    @Test
    void testGetNotificationsStats() {
        // Given
        when(notificationRepository.countTotal()).thenReturn(100L);
        when(notificationRepository.countByStatut(StatutNotification.SENT)).thenReturn(80L);
        when(notificationRepository.countByStatut(StatutNotification.FAILED)).thenReturn(10L);
        when(notificationRepository.countByStatut(StatutNotification.PENDING)).thenReturn(5L);
        when(notificationRepository.countByStatut(StatutNotification.RETRYING)).thenReturn(5L);
        
        // When
        NotificationStatsDTO stats = notificationHistoryService.getNotificationsStats();
        
        // Then
        assertNotNull(stats);
        assertEquals(100L, stats.getTotal());
        assertEquals(80L, stats.getSent());
        assertEquals(10L, stats.getFailed());
        assertEquals(5L, stats.getPending());
        assertEquals(5L, stats.getRetrying());
        // Success rate = (80 / (80 + 10)) * 100 = 88.89%
        assertEquals(88.88888888888889, stats.getSuccessRate(), 0.01);
    }
    
    @Test
    void testGetNotificationsStats_noNotifications() {
        // Given
        when(notificationRepository.countTotal()).thenReturn(0L);
        when(notificationRepository.countByStatut(any())).thenReturn(0L);
        
        // When
        NotificationStatsDTO stats = notificationHistoryService.getNotificationsStats();
        
        // Then
        assertNotNull(stats);
        assertEquals(0L, stats.getTotal());
        assertEquals(0.0, stats.getSuccessRate());
    }
    
    @Test
    void testRetryFailedNotification_success() {
        // Given
        testNotification.setStatut(StatutNotification.FAILED);
        testNotification.setErreurMessage("Previous error");
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        
        // When
        Notification result = notificationHistoryService.retryFailedNotification(1L);
        
        // Then
        assertNotNull(result);
        verify(notificationRepository, times(1)).findById(1L);
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
    
    @Test
    void testRetryFailedNotification_notFound() {
        // Given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            notificationHistoryService.retryFailedNotification(999L);
        });
    }
    
    @Test
    void testRetryFailedNotification_notFailedStatus() {
        // Given
        testNotification.setStatut(StatutNotification.SENT);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        
        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            notificationHistoryService.retryFailedNotification(1L);
        });
    }
    
    @Test
    void testRetryFailedNotification_pendingStatus() {
        // Given
        testNotification.setStatut(StatutNotification.PENDING);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        
        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            notificationHistoryService.retryFailedNotification(1L);
        });
    }
}
