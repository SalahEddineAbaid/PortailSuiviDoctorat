package ma.emsi.inscriptionservice.services;

import ma.emsi.inscriptionservice.DTOs.InscriptionRequest;
import ma.emsi.inscriptionservice.entities.*;
import ma.emsi.inscriptionservice.enums.*;
import ma.emsi.inscriptionservice.exceptions.DerogationRequiredException;
import ma.emsi.inscriptionservice.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test for derogation validation in re-registration workflow
 * Tests Requirement 3.7: Re-registration derogation requirement
 */
@SpringBootTest
@Transactional
class DerogationValidationTest {

    @Autowired
    private InscriptionService inscriptionService;

    @Autowired
    private InscriptionRepository inscriptionRepository;

    @Autowired
    private CampagneRepository campagneRepository;

    @Autowired
    private DerogationRequestRepository derogationRequestRepository;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private DerogationService derogationService;

    private Campagne testCampagne;
    private Inscription premiereInscription;

    @BeforeEach
    void setUp() {
        // Create test campaign
        testCampagne = Campagne.builder()
                .libelle("Test Campaign 2024-2025")
                .type(TypeCampagne.REINSCRIPTION)
                .dateDebut(LocalDate.now().minusDays(10))
                .dateFin(LocalDate.now().plusDays(20))
                .active(true)
                .build();
        testCampagne = campagneRepository.save(testCampagne);

        // Create first inscription from 4 years ago
        premiereInscription = Inscription.builder()
                .doctorantId(100L)
                .directeurTheseId(200L)
                .campagne(testCampagne)
                .sujetThese("Test Thesis")
                .type(TypeInscription.PREMIERE_INSCRIPTION)
                .anneeInscription(2020)
                .statut(StatutInscription.VALIDE)
                .datePremiereInscription(LocalDateTime.now().minusYears(4))
                .build();
        premiereInscription = inscriptionRepository.save(premiereInscription);
    }

    @Test
    void testReInscriptionWithoutDerogationThrowsException() {
        // Given: A student with 4 years of study (> 3 years)
        // And: No approved derogation exists
        when(derogationService.getDerogation(any()))
                .thenReturn(Optional.empty());

        // When: Student attempts re-registration
        InscriptionRequest request = new InscriptionRequest();
        request.setDoctorantId(100L);
        request.setDirecteurTheseId(200L);
        request.setCampagneId(testCampagne.getId());
        request.setSujetThese("Test Thesis");
        request.setType(TypeInscription.REINSCRIPTION);
        request.setAnneeInscription(2024);

        // Then: DerogationRequiredException should be thrown
        DerogationRequiredException exception = assertThrows(
                DerogationRequiredException.class,
                () -> inscriptionService.creerInscription(request)
        );

        // And: Exception should contain correct information
        assertEquals(premiereInscription.getId(), exception.getInscriptionId());
        assertEquals(100L, exception.getDoctorantId());
        assertTrue(exception.getDureeAnnees() >= 3.0);
        assertTrue(exception.getMessage().contains("dérogation approuvée est requise"));
    }

    @Test
    void testReInscriptionWithApprovedDerogationSucceeds() {
        // Given: A student with 4 years of study (> 3 years)
        // And: An approved derogation exists
        DerogationRequest approvedDerogation = DerogationRequest.builder()
                .inscription(premiereInscription)
                .motif("Valid reason")
                .statut(StatutDerogation.APPROUVE_PED)
                .dateDemande(LocalDateTime.now().minusDays(30))
                .build();

        when(derogationService.getDerogation(premiereInscription.getId()))
                .thenReturn(Optional.of(approvedDerogation));

        // When: Student attempts re-registration
        InscriptionRequest request = new InscriptionRequest();
        request.setDoctorantId(100L);
        request.setDirecteurTheseId(200L);
        request.setCampagneId(testCampagne.getId());
        request.setSujetThese("Test Thesis");
        request.setType(TypeInscription.REINSCRIPTION);
        request.setAnneeInscription(2024);
        request.setCin("AB123456");
        request.setCne("R123456789");
        request.setTelephone("0612345678");
        request.setAdresse("Test Address");
        request.setVille("Casablanca");
        request.setPays("Maroc");
        request.setDateNaissance(LocalDate.of(1995, 1, 1));
        request.setLieuNaissance("Casablanca");
        request.setNationalite("Marocaine");
        request.setTitreThese("Test Thesis Title");
        request.setDiscipline("Computer Science");
        request.setLaboratoire("Test Lab");
        request.setEtablissementAccueil("EMSI");
        request.setCotutelle(false);
        request.setDateDebutPrevue(LocalDate.now());

        // Then: Re-registration should succeed
        assertDoesNotThrow(() -> inscriptionService.creerInscription(request));
    }

    @Test
    void testReInscriptionWithRejectedDerogationThrowsException() {
        // Given: A student with 4 years of study (> 3 years)
        // And: A rejected derogation exists
        DerogationRequest rejectedDerogation = DerogationRequest.builder()
                .inscription(premiereInscription)
                .motif("Valid reason")
                .statut(StatutDerogation.REJETE)
                .dateDemande(LocalDateTime.now().minusDays(30))
                .build();

        when(derogationService.getDerogation(premiereInscription.getId()))
                .thenReturn(Optional.of(rejectedDerogation));

        // When: Student attempts re-registration
        InscriptionRequest request = new InscriptionRequest();
        request.setDoctorantId(100L);
        request.setDirecteurTheseId(200L);
        request.setCampagneId(testCampagne.getId());
        request.setSujetThese("Test Thesis");
        request.setType(TypeInscription.REINSCRIPTION);
        request.setAnneeInscription(2024);

        // Then: DerogationRequiredException should be thrown (rejected derogation doesn't count)
        assertThrows(
                DerogationRequiredException.class,
                () -> inscriptionService.creerInscription(request)
        );
    }

    @Test
    void testReInscriptionUnder3YearsDoesNotRequireDerogation() {
        // Given: A student with 2 years of study (< 3 years)
        Inscription recentInscription = Inscription.builder()
                .doctorantId(101L)
                .directeurTheseId(200L)
                .campagne(testCampagne)
                .sujetThese("Test Thesis")
                .type(TypeInscription.PREMIERE_INSCRIPTION)
                .anneeInscription(2022)
                .statut(StatutInscription.VALIDE)
                .datePremiereInscription(LocalDateTime.now().minusYears(2))
                .build();
        recentInscription = inscriptionRepository.save(recentInscription);

        // When: Student attempts re-registration
        InscriptionRequest request = new InscriptionRequest();
        request.setDoctorantId(101L);
        request.setDirecteurTheseId(200L);
        request.setCampagneId(testCampagne.getId());
        request.setSujetThese("Test Thesis");
        request.setType(TypeInscription.REINSCRIPTION);
        request.setAnneeInscription(2024);
        request.setCin("AB123457");
        request.setCne("R123456790");
        request.setTelephone("0612345679");
        request.setAdresse("Test Address");
        request.setVille("Casablanca");
        request.setPays("Maroc");
        request.setDateNaissance(LocalDate.of(1995, 1, 1));
        request.setLieuNaissance("Casablanca");
        request.setNationalite("Marocaine");
        request.setTitreThese("Test Thesis Title");
        request.setDiscipline("Computer Science");
        request.setLaboratoire("Test Lab");
        request.setEtablissementAccueil("EMSI");
        request.setCotutelle(false);
        request.setDateDebutPrevue(LocalDate.now());

        // Then: Re-registration should succeed without derogation
        assertDoesNotThrow(() -> inscriptionService.creerInscription(request));
        
        // And: Derogation service should not be called
        verify(derogationService, never()).getDerogation(any());
    }
}
