package ma.emsi.inscriptionservice.services;

import ma.emsi.inscriptionservice.DTOs.NotificationDTO;
import ma.emsi.inscriptionservice.DTOs.UserDTO;
import ma.emsi.inscriptionservice.client.UserServiceClient;
import ma.emsi.inscriptionservice.entities.AlerteDuree;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.enums.TypeAlerte;
import ma.emsi.inscriptionservice.enums.TypeInscription;
import ma.emsi.inscriptionservice.repositories.AlerteDureeRepository;
import ma.emsi.inscriptionservice.repositories.InscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlerteServiceTest {

    @Mock
    private AlerteDureeRepository alerteDureeRepository;

    @Mock
    private InscriptionRepository inscriptionRepository;

    @Mock
    private KafkaTemplate<String, NotificationDTO> kafkaTemplate;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private AlerteService alerteService;

    private Inscription inscription;
    private UserDTO doctorant;

    @BeforeEach
    void setUp() {
        // Set configuration values
        ReflectionTestUtils.setField(alerteService, "seuil3Ans", 2.5);
        ReflectionTestUtils.setField(alerteService, "seuil6Ans", 5.5);
        ReflectionTestUtils.setField(alerteService, "limiteMax", 6.0);
        ReflectionTestUtils.setField(alerteService, "notificationTopic", "notifications");

        // Create test inscription
        inscription = Inscription.builder()
                .id(1L)
                .doctorantId(100L)
                .directeurTheseId(200L)
                .type(TypeInscription.PREMIERE_INSCRIPTION)
                .anneeInscription(2021)
                .statut(StatutInscription.VALIDE)
                .datePremiereInscription(LocalDateTime.now().minusYears(3))
                .bloqueReInscription(false)
                .build();

        // Create test doctorant
        doctorant = new UserDTO();
        doctorant.setId(100L);
        doctorant.setEmail("doctorant@test.com");
        doctorant.setFirstName("Jean");
        doctorant.setLastName("Dupont");
    }

    @Test
    void testVerifierEtGenererAlertes_Approche3Ans() {
        // Given: inscription with 2.6 years duration
        inscription.setDatePremiereInscription(LocalDateTime.now().minusDays((int)(2.6 * 365.25)));
        when(alerteDureeRepository.countByInscriptionIdAndType(1L, TypeAlerte.APPROCHE_3_ANS)).thenReturn(0L);
        when(userServiceClient.getUserById(100L)).thenReturn(doctorant);

        // When
        alerteService.verifierEtGenererAlertes(inscription);

        // Then
        verify(alerteDureeRepository).save(any(AlerteDuree.class));
        verify(kafkaTemplate).send(eq("notifications"), any(NotificationDTO.class));
    }

    @Test
    void testVerifierEtGenererAlertes_Approche6Ans() {
        // Given: inscription with 5.6 years duration
        inscription.setDatePremiereInscription(LocalDateTime.now().minusDays((int)(5.6 * 365.25)));
        when(alerteDureeRepository.countByInscriptionIdAndType(1L, TypeAlerte.APPROCHE_3_ANS)).thenReturn(1L);
        when(alerteDureeRepository.countByInscriptionIdAndType(1L, TypeAlerte.APPROCHE_6_ANS)).thenReturn(0L);
        when(userServiceClient.getUserById(100L)).thenReturn(doctorant);

        // When
        alerteService.verifierEtGenererAlertes(inscription);

        // Then
        verify(alerteDureeRepository).save(any(AlerteDuree.class));
        verify(kafkaTemplate).send(eq("notifications"), any(NotificationDTO.class));
    }

    @Test
    void testVerifierEtGenererAlertes_Depasse6Ans() {
        // Given: inscription with 6.1 years duration
        inscription.setDatePremiereInscription(LocalDateTime.now().minusDays((int)(6.1 * 365.25)));
        when(alerteDureeRepository.countByInscriptionIdAndType(1L, TypeAlerte.APPROCHE_3_ANS)).thenReturn(1L);
        when(alerteDureeRepository.countByInscriptionIdAndType(1L, TypeAlerte.APPROCHE_6_ANS)).thenReturn(1L);
        when(alerteDureeRepository.countByInscriptionIdAndType(1L, TypeAlerte.DEPASSE_6_ANS)).thenReturn(0L);
        when(userServiceClient.getUserById(100L)).thenReturn(doctorant);
        when(inscriptionRepository.save(any(Inscription.class))).thenReturn(inscription);

        // When
        alerteService.verifierEtGenererAlertes(inscription);

        // Then
        verify(alerteDureeRepository).save(any(AlerteDuree.class));
        verify(inscriptionRepository).save(inscription);
        assertTrue(inscription.isBloqueReInscription());
        verify(kafkaTemplate).send(eq("notifications"), any(NotificationDTO.class));
    }

    @Test
    void testVerifierEtGenererAlertes_NoDuplicateAlerts() {
        // Given: inscription with 2.6 years duration and existing alert
        inscription.setDatePremiereInscription(LocalDateTime.now().minusDays((int)(2.6 * 365.25)));
        when(alerteDureeRepository.countByInscriptionIdAndType(1L, TypeAlerte.APPROCHE_3_ANS)).thenReturn(1L);

        // When
        alerteService.verifierEtGenererAlertes(inscription);

        // Then: no new alert should be created
        verify(alerteDureeRepository, never()).save(any(AlerteDuree.class));
        verify(kafkaTemplate, never()).send(anyString(), any(NotificationDTO.class));
    }

    @Test
    void testVerifierEtGenererAlertes_NoDatePremiereInscription() {
        // Given: inscription without datePremiereInscription
        inscription.setDatePremiereInscription(null);

        // When
        alerteService.verifierEtGenererAlertes(inscription);

        // Then: no alert should be created
        verify(alerteDureeRepository, never()).save(any(AlerteDuree.class));
        verify(kafkaTemplate, never()).send(anyString(), any(NotificationDTO.class));
    }

    @Test
    void testAlerteExiste_ReturnsTrue() {
        // Given
        when(alerteDureeRepository.countByInscriptionIdAndType(1L, TypeAlerte.APPROCHE_3_ANS)).thenReturn(1L);

        // When
        boolean exists = alerteService.alerteExiste(inscription, TypeAlerte.APPROCHE_3_ANS);

        // Then
        assertTrue(exists);
    }

    @Test
    void testAlerteExiste_ReturnsFalse() {
        // Given
        when(alerteDureeRepository.countByInscriptionIdAndType(1L, TypeAlerte.APPROCHE_3_ANS)).thenReturn(0L);

        // When
        boolean exists = alerteService.alerteExiste(inscription, TypeAlerte.APPROCHE_3_ANS);

        // Then
        assertFalse(exists);
    }

    @Test
    void testCreerAlerte() {
        // Given
        when(userServiceClient.getUserById(100L)).thenReturn(doctorant);
        when(alerteDureeRepository.save(any(AlerteDuree.class))).thenAnswer(invocation -> {
            AlerteDuree alerte = invocation.getArgument(0);
            alerte.setId(1L);
            return alerte;
        });

        // When
        alerteService.creerAlerte(inscription, TypeAlerte.APPROCHE_3_ANS);

        // Then
        ArgumentCaptor<AlerteDuree> alerteCaptor = ArgumentCaptor.forClass(AlerteDuree.class);
        verify(alerteDureeRepository).save(alerteCaptor.capture());
        
        AlerteDuree savedAlerte = alerteCaptor.getValue();
        assertEquals(inscription, savedAlerte.getInscription());
        assertEquals(TypeAlerte.APPROCHE_3_ANS, savedAlerte.getType());
        assertFalse(savedAlerte.isTraite());
        assertNotNull(savedAlerte.getAction());

        verify(kafkaTemplate).send(eq("notifications"), any(NotificationDTO.class));
    }

    @Test
    void testGetAlertesActives() {
        // Given
        AlerteDuree alerte1 = AlerteDuree.builder()
                .id(1L)
                .inscription(inscription)
                .type(TypeAlerte.APPROCHE_3_ANS)
                .traite(false)
                .build();
        
        AlerteDuree alerte2 = AlerteDuree.builder()
                .id(2L)
                .inscription(inscription)
                .type(TypeAlerte.APPROCHE_6_ANS)
                .traite(false)
                .build();

        when(alerteDureeRepository.findAlertesActivesByDoctorant(100L))
                .thenReturn(Arrays.asList(alerte1, alerte2));

        // When
        List<AlerteDuree> alertes = alerteService.getAlertesActives(100L);

        // Then
        assertEquals(2, alertes.size());
        verify(alerteDureeRepository).findAlertesActivesByDoctorant(100L);
    }

    @Test
    void testCreerAlerte_KafkaFailure() {
        // Given
        when(userServiceClient.getUserById(100L)).thenThrow(new RuntimeException("Service unavailable"));
        when(alerteDureeRepository.save(any(AlerteDuree.class))).thenAnswer(invocation -> {
            AlerteDuree alerte = invocation.getArgument(0);
            alerte.setId(1L);
            return alerte;
        });

        // When - should not throw exception even if Kafka fails
        assertDoesNotThrow(() -> alerteService.creerAlerte(inscription, TypeAlerte.APPROCHE_3_ANS));

        // Then - alert should still be saved
        verify(alerteDureeRepository).save(any(AlerteDuree.class));
    }
}
