package ma.emsi.inscriptionservice.services;

import ma.emsi.inscriptionservice.DTOs.ValidationRequest;
import ma.emsi.inscriptionservice.entities.*;
import ma.emsi.inscriptionservice.enums.*;
import ma.emsi.inscriptionservice.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test for attestation generation in validation workflow
 * Tests Requirement 2.1: Attestation generation when status changes to VALIDE
 */
@SpringBootTest
@Transactional
class InscriptionServiceIntegrationTest {

    @Autowired
    private InscriptionService inscriptionService;

    @Autowired
    private InscriptionRepository inscriptionRepository;

    @Autowired
    private CampagneRepository campagneRepository;

    @Autowired
    private InfosDoctorantRepository infosDoctorantRepository;

    @Autowired
    private InfosTheseRepository infosTheseRepository;

    @Autowired
    private ValidationInscriptionRepository validationRepository;

    @MockBean
    private NotificationService notificationService;

    private Inscription testInscription;
    private Campagne testCampagne;

    @BeforeEach
    void setUp() {
        // Create test campaign
        testCampagne = Campagne.builder()
                .libelle("Test Campaign 2024-2025")
                .type(TypeCampagne.INSCRIPTION)
                .dateDebut(LocalDate.now().minusDays(10))
                .dateFin(LocalDate.now().plusDays(20))
                .active(true)
                .build();
        testCampagne = campagneRepository.save(testCampagne);

        // Create test inscription
        testInscription = Inscription.builder()
                .doctorantId(1L)
                .directeurTheseId(2L)
                .campagne(testCampagne)
                .sujetThese("Test Thesis Subject")
                .type(TypeInscription.PREMIERE_INSCRIPTION)
                .anneeInscription(2024)
                .statut(StatutInscription.EN_ATTENTE_ADMIN)
                .build();
        testInscription = inscriptionRepository.save(testInscription);

        // Create student information
        InfosDoctorant infosDoctorant = InfosDoctorant.builder()
                .inscription(testInscription)
                .cin("AB123456")
                .cne("R123456789")
                .telephone("0612345678")
                .adresse("Test Address")
                .ville("Casablanca")
                .pays("Maroc")
                .dateNaissance(LocalDate.of(1995, 1, 1))
                .lieuNaissance("Casablanca")
                .nationalite("Marocaine")
                .build();
        infosDoctorantRepository.save(infosDoctorant);

        // Create thesis information
        InfosThese infosThese = InfosThese.builder()
                .inscription(testInscription)
                .titreThese("Test Thesis Title")
                .discipline("Computer Science")
                .laboratoire("Test Lab")
                .etablissementAccueil("EMSI")
                .cotutelle(false)
                .dateDebutPrevue(LocalDate.now())
                .build();
        infosTheseRepository.save(infosThese);

        // Create admin validation record
        ValidationInscription validation = ValidationInscription.builder()
                .inscription(testInscription)
                .validateurId(0L)
                .typeValidateur(TypeValidateur.ADMINISTRATION)
                .statut(StatutValidation.EN_ATTENTE)
                .build();
        validationRepository.save(validation);
    }

    @Test
    void testAttestationGenerationOnValidation() {
        // Given: An inscription in EN_ATTENTE_ADMIN status
        assertEquals(StatutInscription.EN_ATTENTE_ADMIN, testInscription.getStatut());

        // When: Admin validates the inscription
        ValidationRequest request = new ValidationRequest();
        request.setApprouve(true);
        request.setCommentaire("Approved");

        inscriptionService.validerParAdmin(testInscription.getId(), request);

        // Then: Inscription status should be VALIDE
        Inscription updatedInscription = inscriptionRepository.findById(testInscription.getId())
                .orElseThrow();
        assertEquals(StatutInscription.VALIDE, updatedInscription.getStatut());
        assertNotNull(updatedInscription.getDateValidation());

        // And: Attestation generation should have been called
        verify(notificationService, times(1))
                .genererAttestationInscription(testInscription.getId());

        // And: Validation notification should have been sent
        verify(notificationService, times(1))
                .notifierValidationDefinitive(
                        eq(testInscription.getDoctorantId()),
                        eq(testInscription.getDirecteurTheseId()),
                        eq(testInscription.getId())
                );
    }

    @Test
    void testNoAttestationGenerationOnRejection() {
        // Given: An inscription in EN_ATTENTE_ADMIN status
        assertEquals(StatutInscription.EN_ATTENTE_ADMIN, testInscription.getStatut());

        // When: Admin rejects the inscription
        ValidationRequest request = new ValidationRequest();
        request.setApprouve(false);
        request.setCommentaire("Rejected");

        inscriptionService.validerParAdmin(testInscription.getId(), request);

        // Then: Inscription status should be REJETE
        Inscription updatedInscription = inscriptionRepository.findById(testInscription.getId())
                .orElseThrow();
        assertEquals(StatutInscription.REJETE, updatedInscription.getStatut());

        // And: Attestation generation should NOT have been called
        verify(notificationService, never())
                .genererAttestationInscription(any());

        // And: Rejection notification should have been sent
        verify(notificationService, times(1))
                .notifierDoctorantRejet(
                        eq(testInscription.getDoctorantId()),
                        eq(testInscription.getId())
                );
    }

    @Test
    void testAttestationGenerationErrorDoesNotBlockValidation() {
        // Given: An inscription in EN_ATTENTE_ADMIN status
        // And: Attestation generation will fail
        doThrow(new RuntimeException("Attestation generation failed"))
                .when(notificationService)
                .genererAttestationInscription(any());

        // When: Admin validates the inscription
        ValidationRequest request = new ValidationRequest();
        request.setApprouve(true);
        request.setCommentaire("Approved");

        // Then: Validation should succeed despite attestation generation failure
        assertDoesNotThrow(() -> 
                inscriptionService.validerParAdmin(testInscription.getId(), request)
        );

        // And: Inscription status should still be VALIDE
        Inscription updatedInscription = inscriptionRepository.findById(testInscription.getId())
                .orElseThrow();
        assertEquals(StatutInscription.VALIDE, updatedInscription.getStatut());
    }
}
