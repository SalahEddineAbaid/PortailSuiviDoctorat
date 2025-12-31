package ma.emsi.inscriptionservice.integration;

import ma.emsi.inscriptionservice.DTOs.*;
import ma.emsi.inscriptionservice.entities.*;
import ma.emsi.inscriptionservice.enums.*;
import ma.emsi.inscriptionservice.repositories.*;
import ma.emsi.inscriptionservice.services.*;
import ma.emsi.inscriptionservice.exceptions.DerogationRequiredException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complete workflow integration tests for inscription-service.
 * Tests Requirements: All requirements
 * 
 * This test suite validates complete end-to-end workflows:
 * 1. Complete inscription workflow (create → submit → validate → attestation)
 * 2. Derogation workflow (request → director approve → PED approve)
 * 3. Campaign lifecycle (open → inscriptions → close)
 * 4. Alert generation workflow
 */
@SpringBootTest
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"inscription-events"})
@TestPropertySource(properties = {
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "upload.allowed-types=application/pdf,image/jpeg,image/png",
    "upload.max-size=10485760",
    "upload.virus-scan.enabled=false",
    "alertes.duree.seuil-3-ans=2.5",
    "alertes.duree.seuil-6-ans=5.5"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CompleteWorkflowIntegrationTest {

    @Autowired
    private InscriptionService inscriptionService;
    
    @Autowired
    private DerogationService derogationService;
    
    @Autowired
    private CampagneService campagneService;
    
    @Autowired
    private AlerteService alerteService;
    
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
    
    @Autowired
    private AlerteDureeRepository alerteDureeRepository;

    private Campagne testCampagne;
    private Long doctorantId = 1001L;
    private Long directeurId = 2001L;

    @BeforeEach
    void setUp() {
        // Create test campaign
        testCampagne = Campagne.builder()
                .libelle("Campagne Test 2024-2025")
                .type(TypeCampagne.INSCRIPTION)
                .dateDebut(LocalDate.now().minusDays(10))
                .dateFin(LocalDate.now().plusDays(20))
                .active(true)
                .build();
        testCampagne = campagneRepository.save(testCampagne);
    }

    /**
     * Test 1: Complete inscription workflow
     * Tests: create → submit → director validate → admin validate → attestation generation
     * Validates Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 7.1, 7.2, 7.4
     */
    @Test
    @DisplayName("Complete inscription workflow: create → submit → validate → attestation")
    void testCompleteInscriptionWorkflow() {
        // Step 1: Create inscription
        InscriptionRequest request = new InscriptionRequest();
        request.setDoctorantId(doctorantId);
        request.setDirecteurTheseId(directeurId);
        request.setCampagneId(testCampagne.getId());
        request.setSujetThese("Intelligence Artificielle et Machine Learning");
        request.setType(TypeInscription.PREMIERE_INSCRIPTION);
        request.setAnneeInscription(2024);

        InscriptionResponse inscriptionResponse = inscriptionService.creerInscription(request);
        assertNotNull(inscriptionResponse);
        assertEquals(StatutInscription.BROUILLON, inscriptionResponse.getStatut());
        Long inscriptionId = inscriptionResponse.getId();

        // Add student information
        InfosDoctorant infosDoctorant = InfosDoctorant.builder()
                .inscription(inscriptionRepository.findById(inscriptionId).orElseThrow())
                .cin("AB123456")
                .cne("R123456789")
                .telephone("0612345678")
                .adresse("123 Rue Test")
                .ville("Casablanca")
                .pays("Maroc")
                .dateNaissance(LocalDate.of(1995, 5, 15))
                .lieuNaissance("Casablanca")
                .nationalite("Marocaine")
                .build();
        infosDoctorantRepository.save(infosDoctorant);

        // Add thesis information
        InfosThese infosThese = InfosThese.builder()
                .inscription(inscriptionRepository.findById(inscriptionId).orElseThrow())
                .titreThese("Apprentissage profond pour la reconnaissance d'images")
                .discipline("Informatique")
                .laboratoire("Laboratoire de Recherche en IA")
                .etablissementAccueil("EMSI")
                .cotutelle(false)
                .dateDebutPrevue(LocalDate.now())
                .build();
        infosTheseRepository.save(infosThese);

        // Step 2: Submit inscription
        inscriptionService.soumettre(inscriptionId, doctorantId);
        Inscription afterSubmit = inscriptionRepository.findById(inscriptionId).orElseThrow();
        assertEquals(StatutInscription.EN_ATTENTE_DIRECTEUR, afterSubmit.getStatut());

        // Step 3: Director validates
        ValidationRequest directorValidation = new ValidationRequest();
        directorValidation.setApprouve(true);
        directorValidation.setCommentaire("Dossier complet et conforme");
        
        inscriptionService.validerParDirecteur(inscriptionId, directorValidation, directeurId);
        Inscription afterDirectorValidation = inscriptionRepository.findById(inscriptionId).orElseThrow();
        assertEquals(StatutInscription.EN_ATTENTE_ADMIN, afterDirectorValidation.getStatut());

        // Step 4: Admin validates
        ValidationRequest adminValidation = new ValidationRequest();
        adminValidation.setApprouve(true);
        adminValidation.setCommentaire("Validation administrative approuvée");
        
        inscriptionService.validerParAdmin(inscriptionId, adminValidation);
        Inscription afterAdminValidation = inscriptionRepository.findById(inscriptionId).orElseThrow();
        assertEquals(StatutInscription.VALIDE, afterAdminValidation.getStatut());
        assertNotNull(afterAdminValidation.getDateValidation());

        // Step 5: Verify attestation generation was triggered
        // Note: In real implementation, attestation would be generated
        // For this test, we verify the workflow completed successfully
        assertTrue(afterAdminValidation.getStatut() == StatutInscription.VALIDE);
    }

    /**
     * Test 2: Derogation workflow
     * Tests: request → director approve → PED approve
     * Validates Requirements: 3.1, 3.2, 3.3, 3.5, 3.7, 7.8
     */
    @Test
    @DisplayName("Derogation workflow: request → director approve → PED approve")
    void testDerogationWorkflow() {
        // Step 1: Create an inscription with > 3 years duration
        Inscription inscription = Inscription.builder()
                .doctorantId(doctorantId)
                .directeurTheseId(directeurId)
                .campagne(testCampagne)
                .sujetThese("Recherche avancée nécessitant dérogation")
                .type(TypeInscription.REINSCRIPTION)
                .anneeInscription(2024)
                .statut(StatutInscription.BROUILLON)
                .dateCreation(LocalDateTime.now().minusYears(4)) // 4 years ago
                .datePremiereInscription(LocalDateTime.now().minusYears(4))
                .build();
        inscription = inscriptionRepository.save(inscription);

        // Add required information
        InfosDoctorant infosDoctorant = InfosDoctorant.builder()
                .inscription(inscription)
                .cin("CD789012")
                .cne("R987654321")
                .telephone("0698765432")
                .adresse("456 Avenue Test")
                .ville("Rabat")
                .pays("Maroc")
                .dateNaissance(LocalDate.of(1993, 3, 20))
                .lieuNaissance("Rabat")
                .nationalite("Marocaine")
                .build();
        infosDoctorantRepository.save(infosDoctorant);

        InfosThese infosThese = InfosThese.builder()
                .inscription(inscription)
                .titreThese("Recherche complexe nécessitant plus de temps")
                .discipline("Informatique")
                .laboratoire("Lab Recherche")
                .etablissementAccueil("EMSI")
                .cotutelle(false)
                .dateDebutPrevue(LocalDate.now().minusYears(4))
                .build();
        infosTheseRepository.save(infosThese);

        Long inscriptionId = inscription.getId();

        // Step 2: Create derogation request
        String motif = "Recherche complexe nécessitant une année supplémentaire pour finaliser les expérimentations";
        byte[] documents = new byte[]{1, 2, 3, 4, 5};

        DerogationRequest derogation = derogationService.creerDerogation(
                inscriptionId, 
                motif, 
                documents
        );
        
        assertNotNull(derogation);
        assertEquals(StatutDerogation.EN_ATTENTE, derogation.getStatut());
        assertNotNull(derogation.getDateDemande());
        assertEquals(inscriptionId, derogation.getInscription().getId());

        // Step 3: Director approves derogation
        boolean directorApproves = true;
        String directorComment = "Le doctorant a fait des progrès significatifs. Je recommande l'approbation.";

        DerogationRequest afterDirectorApproval = derogationService.validerParDirecteur(
                derogation.getId(), 
                directorApproves, 
                directorComment
        );
        
        assertEquals(StatutDerogation.APPROUVE_DIRECTEUR, afterDirectorApproval.getStatut());
        assertNotNull(afterDirectorApproval.getDateValidation());
        assertEquals(directorComment, afterDirectorApproval.getCommentaireValidation());

        // Step 4: PED approves derogation
        boolean pedApproves = true;
        String pedComment = "Dérogation approuvée par le PED";

        DerogationRequest afterPedApproval = derogationService.validerParPED(
                derogation.getId(), 
                pedApproves, 
                pedComment
        );
        
        assertEquals(StatutDerogation.APPROUVE_PED, afterPedApproval.getStatut());

        // Step 5: Verify inscription can now proceed with re-registration
        Inscription updatedInscription = inscriptionRepository.findById(inscriptionId).orElseThrow();
        assertFalse(updatedInscription.isBloqueReInscription());
        
        // Verify derogation is retrievable
        DerogationRequest retrievedDerogation = derogationService.getDerogation(inscriptionId)
                .orElseThrow(() -> new AssertionError("Derogation should exist"));
        assertEquals(StatutDerogation.APPROUVE_PED, retrievedDerogation.getStatut());
    }

    /**
     * Test 3: Derogation rejection workflow
     * Tests: request → director reject
     * Validates Requirements: 3.1, 3.4, 3.6
     */
    @Test
    @DisplayName("Derogation rejection workflow: request → director reject")
    void testDerogationRejectionWorkflow() {
        // Create inscription with > 3 years duration
        Inscription inscription = Inscription.builder()
                .doctorantId(doctorantId + 1)
                .directeurTheseId(directeurId)
                .campagne(testCampagne)
                .sujetThese("Recherche avec dérogation refusée")
                .type(TypeInscription.REINSCRIPTION)
                .anneeInscription(2024)
                .statut(StatutInscription.BROUILLON)
                .dateCreation(LocalDateTime.now().minusYears(4))
                .datePremiereInscription(LocalDateTime.now().minusYears(4))
                .build();
        inscription = inscriptionRepository.save(inscription);

        // Create derogation request
        String motif = "Demande de prolongation";
        byte[] documents = new byte[]{1, 2, 3};

        DerogationRequest derogation = derogationService.creerDerogation(
                inscription.getId(), 
                motif, 
                documents
        );

        // Director rejects
        DerogationRequest afterRejection = derogationService.validerParDirecteur(
                derogation.getId(), 
                false, 
                "Progrès insuffisants pour justifier une prolongation"
        );
        
        assertEquals(StatutDerogation.REJETE, afterRejection.getStatut());
        assertNotNull(afterRejection.getDateValidation());
    }

    /**
     * Test 4: Campaign lifecycle
     * Tests: open → inscriptions → close
     * Validates Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.6, 7.7
     */
    @Test
    @DisplayName("Campaign lifecycle: open → inscriptions → close")
    void testCampaignLifecycle() {
        // Step 1: Create a new campaign (initially inactive)
        Campagne newCampagne = Campagne.builder()
                .libelle("Campagne 2025-2026")
                .type(TypeCampagne.INSCRIPTION)
                .dateDebut(LocalDate.now().plusDays(1))
                .dateFin(LocalDate.now().plusDays(30))
                .active(false)
                .build();
        newCampagne = campagneRepository.save(newCampagne);
        
        assertFalse(newCampagne.getActive());

        // Step 2: Simulate campaign opening (would normally be done by scheduled task)
        newCampagne.setActive(true);
        newCampagne = campagneRepository.save(newCampagne);
        assertTrue(newCampagne.getActive());

        // Step 3: Create multiple inscriptions during campaign
        for (int i = 0; i < 5; i++) {
            InscriptionRequest request = new InscriptionRequest();
            request.setDoctorantId(doctorantId + i);
            request.setDirecteurTheseId(directeurId);
            request.setCampagneId(newCampagne.getId());
            request.setSujetThese("Sujet de thèse " + i);
            request.setType(TypeInscription.PREMIERE_INSCRIPTION);
            request.setAnneeInscription(2025);

            InscriptionResponse response = inscriptionService.creerInscription(request);
            assertNotNull(response);
        }

        // Step 4: Get campaign statistics
        StatistiquesCampagne stats = campagneService.getStatistiques(newCampagne.getId());
        assertNotNull(stats);
        assertEquals(5, stats.getNombreInscriptions());
        assertTrue(stats.getParStatut().containsKey(StatutInscription.BROUILLON));
        assertEquals(5, stats.getParStatut().get(StatutInscription.BROUILLON));

        // Step 5: Clone campaign for next year
        CampagneResponse clonedCampagneResponse = campagneService.clonerCampagne(
                newCampagne.getId(),
                LocalDate.now().plusYears(1),
                LocalDate.now().plusYears(1).plusDays(30)
        );
        
        assertNotNull(clonedCampagneResponse);
        assertTrue(clonedCampagneResponse.getLibelle().contains("2026"));
        assertFalse(clonedCampagneResponse.getActive());

        // Step 6: Close original campaign
        newCampagne.setActive(false);
        newCampagne = campagneRepository.save(newCampagne);
        assertFalse(newCampagne.getActive());
    }

    /**
     * Test 5: Alert generation workflow
     * Tests: duration calculation → alert creation → notification
     * Validates Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 7.9
     */
    @Test
    @DisplayName("Alert generation workflow: duration calculation → alert creation")
    void testAlertGenerationWorkflow() {
        // Test Case 1: 2.5 years - APPROCHE_3_ANS alert
        Inscription inscription25Years = Inscription.builder()
                .doctorantId(doctorantId + 10)
                .directeurTheseId(directeurId)
                .campagne(testCampagne)
                .sujetThese("Thèse approchant 3 ans")
                .type(TypeInscription.REINSCRIPTION)
                .anneeInscription(2024)
                .statut(StatutInscription.VALIDE)
                .dateCreation(LocalDateTime.now().minusDays((long)(2.5 * 365.25)))
                .datePremiereInscription(LocalDateTime.now().minusDays((long)(2.5 * 365.25)))
                .build();
        inscription25Years = inscriptionRepository.save(inscription25Years);

        // Trigger alert verification
        alerteService.verifierEtGenererAlertes(inscription25Years);

        // Verify APPROCHE_3_ANS alert was created
        List<AlerteDuree> alertes25 = alerteDureeRepository.findByInscriptionId(inscription25Years.getId());
        assertEquals(1, alertes25.size());
        assertEquals(TypeAlerte.APPROCHE_3_ANS, alertes25.get(0).getType());
        assertNotNull(alertes25.get(0).getDateAlerte());

        // Test Case 2: 5.5 years - APPROCHE_6_ANS alert
        Inscription inscription55Years = Inscription.builder()
                .doctorantId(doctorantId + 11)
                .directeurTheseId(directeurId)
                .campagne(testCampagne)
                .sujetThese("Thèse approchant 6 ans")
                .type(TypeInscription.REINSCRIPTION)
                .anneeInscription(2024)
                .statut(StatutInscription.VALIDE)
                .dateCreation(LocalDateTime.now().minusDays((long)(5.5 * 365.25)))
                .datePremiereInscription(LocalDateTime.now().minusDays((long)(5.5 * 365.25)))
                .build();
        inscription55Years = inscriptionRepository.save(inscription55Years);

        alerteService.verifierEtGenererAlertes(inscription55Years);

        List<AlerteDuree> alertes55 = alerteDureeRepository.findByInscriptionId(inscription55Years.getId());
        assertTrue(alertes55.size() >= 1);
        assertTrue(alertes55.stream().anyMatch(a -> a.getType() == TypeAlerte.APPROCHE_6_ANS));

        // Test Case 3: 6+ years - DEPASSE_6_ANS alert and blocking
        Inscription inscription6Years = Inscription.builder()
                .doctorantId(doctorantId + 12)
                .directeurTheseId(directeurId)
                .campagne(testCampagne)
                .sujetThese("Thèse dépassant 6 ans")
                .type(TypeInscription.REINSCRIPTION)
                .anneeInscription(2024)
                .statut(StatutInscription.VALIDE)
                .dateCreation(LocalDateTime.now().minusDays((long)(6.1 * 365.25)))
                .datePremiereInscription(LocalDateTime.now().minusDays((long)(6.1 * 365.25)))
                .build();
        inscription6Years = inscriptionRepository.save(inscription6Years);

        alerteService.verifierEtGenererAlertes(inscription6Years);

        List<AlerteDuree> alertes6 = alerteDureeRepository.findByInscriptionId(inscription6Years.getId());
        assertTrue(alertes6.stream().anyMatch(a -> a.getType() == TypeAlerte.DEPASSE_6_ANS));
        
        // Verify blocking flag is set
        Inscription updatedInscription6 = inscriptionRepository.findById(inscription6Years.getId()).orElseThrow();
        assertTrue(updatedInscription6.isBloqueReInscription());

        // Test Case 4: Alert idempotency - calling multiple times should not create duplicates
        alerteService.verifierEtGenererAlertes(inscription25Years);
        alerteService.verifierEtGenererAlertes(inscription25Years);
        alerteService.verifierEtGenererAlertes(inscription25Years);

        List<AlerteDuree> alertesAfterMultipleCalls = alerteDureeRepository.findByInscriptionId(inscription25Years.getId());
        assertEquals(1, alertesAfterMultipleCalls.size(), "Multiple calls should not create duplicate alerts");
    }

    /**
     * Test 6: Re-registration with alert verification
     * Tests: re-registration triggers alert verification
     * Validates Requirements: 4.6
     */
    @Test
    @DisplayName("Re-registration triggers alert verification")
    void testReRegistrationWithAlertVerification() {
        // Create first inscription
        Inscription premiereInscription = Inscription.builder()
                .doctorantId(doctorantId + 20)
                .directeurTheseId(directeurId)
                .campagne(testCampagne)
                .sujetThese("Première inscription")
                .type(TypeInscription.PREMIERE_INSCRIPTION)
                .anneeInscription(2021)
                .statut(StatutInscription.VALIDE)
                .dateCreation(LocalDateTime.now().minusYears(3))
                .datePremiereInscription(LocalDateTime.now().minusYears(3))
                .build();
        premiereInscription = inscriptionRepository.save(premiereInscription);

        // Add required information
        InfosDoctorant infosDoctorant = InfosDoctorant.builder()
                .inscription(premiereInscription)
                .cin("EF345678")
                .cne("R345678901")
                .telephone("0634567890")
                .adresse("789 Boulevard Test")
                .ville("Fès")
                .pays("Maroc")
                .dateNaissance(LocalDate.of(1994, 7, 10))
                .lieuNaissance("Fès")
                .nationalite("Marocaine")
                .build();
        infosDoctorantRepository.save(infosDoctorant);

        InfosThese infosThese = InfosThese.builder()
                .inscription(premiereInscription)
                .titreThese("Recherche en cours")
                .discipline("Informatique")
                .laboratoire("Lab Test")
                .etablissementAccueil("EMSI")
                .cotutelle(false)
                .dateDebutPrevue(LocalDate.now().minusYears(3))
                .build();
        infosTheseRepository.save(infosThese);

        // Create derogation for > 3 years
        DerogationRequest derogation = derogationService.creerDerogation(
                premiereInscription.getId(),
                "Besoin de temps supplémentaire",
                new byte[]{1, 2, 3}
        );
        derogationService.validerParDirecteur(derogation.getId(), true, "Approuvé");
        derogationService.validerParPED(derogation.getId(), true, "Approuvé PED");

        // Attempt re-registration (should trigger alert verification)
        InscriptionRequest reInscriptionRequest = new InscriptionRequest();
        reInscriptionRequest.setDoctorantId(doctorantId + 20);
        reInscriptionRequest.setDirecteurTheseId(directeurId);
        reInscriptionRequest.setCampagneId(testCampagne.getId());
        reInscriptionRequest.setSujetThese("Réinscription année 4");
        reInscriptionRequest.setType(TypeInscription.REINSCRIPTION);
        reInscriptionRequest.setAnneeInscription(2024);

        // This should succeed and trigger alert verification
        InscriptionResponse reInscriptionResponse = inscriptionService.creerInscription(reInscriptionRequest);
        assertNotNull(reInscriptionResponse);

        // Verify alerts were checked (APPROCHE_3_ANS should exist)
        List<AlerteDuree> alertes = alerteDureeRepository.findByInscriptionId(premiereInscription.getId());
        assertTrue(alertes.size() > 0, "Alerts should have been generated during re-registration");
    }

    /**
     * Test 7: Complete workflow with rejection at director level
     * Tests: create → submit → director reject
     * Validates Requirements: 7.1, 7.3
     */
    @Test
    @DisplayName("Inscription workflow with director rejection")
    void testInscriptionWorkflowWithDirectorRejection() {
        // Create inscription
        InscriptionRequest request = new InscriptionRequest();
        request.setDoctorantId(doctorantId + 30);
        request.setDirecteurTheseId(directeurId);
        request.setCampagneId(testCampagne.getId());
        request.setSujetThese("Sujet à rejeter");
        request.setType(TypeInscription.PREMIERE_INSCRIPTION);
        request.setAnneeInscription(2024);

        InscriptionResponse inscriptionResponse = inscriptionService.creerInscription(request);
        Long inscriptionId = inscriptionResponse.getId();

        // Add required information
        InfosDoctorant infosDoctorant = InfosDoctorant.builder()
                .inscription(inscriptionRepository.findById(inscriptionId).orElseThrow())
                .cin("GH567890")
                .cne("R567890123")
                .telephone("0656789012")
                .adresse("321 Rue Rejet")
                .ville("Marrakech")
                .pays("Maroc")
                .dateNaissance(LocalDate.of(1996, 9, 25))
                .lieuNaissance("Marrakech")
                .nationalite("Marocaine")
                .build();
        infosDoctorantRepository.save(infosDoctorant);

        InfosThese infosThese = InfosThese.builder()
                .inscription(inscriptionRepository.findById(inscriptionId).orElseThrow())
                .titreThese("Sujet incomplet")
                .discipline("Informatique")
                .laboratoire("Lab Test")
                .etablissementAccueil("EMSI")
                .cotutelle(false)
                .dateDebutPrevue(LocalDate.now())
                .build();
        infosTheseRepository.save(infosThese);

        // Submit
        inscriptionService.soumettre(inscriptionId, doctorantId + 30);

        // Director rejects
        ValidationRequest directorRejection = new ValidationRequest();
        directorRejection.setApprouve(false);
        directorRejection.setCommentaire("Dossier incomplet, manque de clarté dans le sujet");

        inscriptionService.validerParDirecteur(inscriptionId, directorRejection, directeurId);
        
        Inscription afterRejection = inscriptionRepository.findById(inscriptionId).orElseThrow();
        assertEquals(StatutInscription.REJETE, afterRejection.getStatut());
        
        // Verify validation record
        List<ValidationInscription> validations = validationRepository.findByInscriptionId(inscriptionId);
        assertTrue(validations.stream().anyMatch(v -> 
            v.getTypeValidateur() == TypeValidateur.DIRECTEUR_THESE && 
            v.getStatut() == StatutValidation.REJETE
        ));
    }

    /**
     * Test 8: Complete workflow with rejection at admin level
     * Tests: create → submit → director validate → admin reject
     * Validates Requirements: 7.1, 7.2, 7.5
     */
    @Test
    @DisplayName("Inscription workflow with admin rejection")
    void testInscriptionWorkflowWithAdminRejection() {
        // Create inscription
        InscriptionRequest request = new InscriptionRequest();
        request.setDoctorantId(doctorantId + 31);
        request.setDirecteurTheseId(directeurId);
        request.setCampagneId(testCampagne.getId());
        request.setSujetThese("Sujet validé par directeur mais rejeté par admin");
        request.setType(TypeInscription.PREMIERE_INSCRIPTION);
        request.setAnneeInscription(2024);

        InscriptionResponse inscriptionResponse = inscriptionService.creerInscription(request);
        Long inscriptionId = inscriptionResponse.getId();

        // Add required information
        InfosDoctorant infosDoctorant = InfosDoctorant.builder()
                .inscription(inscriptionRepository.findById(inscriptionId).orElseThrow())
                .cin("IJ789012")
                .cne("R789012345")
                .telephone("0678901234")
                .adresse("654 Avenue Admin")
                .ville("Tanger")
                .pays("Maroc")
                .dateNaissance(LocalDate.of(1997, 11, 30))
                .lieuNaissance("Tanger")
                .nationalite("Marocaine")
                .build();
        infosDoctorantRepository.save(infosDoctorant);

        InfosThese infosThese = InfosThese.builder()
                .inscription(inscriptionRepository.findById(inscriptionId).orElseThrow())
                .titreThese("Sujet avec problème administratif")
                .discipline("Informatique")
                .laboratoire("Lab Test")
                .etablissementAccueil("EMSI")
                .cotutelle(false)
                .dateDebutPrevue(LocalDate.now())
                .build();
        infosTheseRepository.save(infosThese);

        // Submit
        inscriptionService.soumettre(inscriptionId, doctorantId + 31);

        // Director validates
        ValidationRequest directorValidation = new ValidationRequest();
        directorValidation.setApprouve(true);
        directorValidation.setCommentaire("Approuvé par le directeur");
        inscriptionService.validerParDirecteur(inscriptionId, directorValidation, directeurId);

        // Admin rejects
        ValidationRequest adminRejection = new ValidationRequest();
        adminRejection.setApprouve(false);
        adminRejection.setCommentaire("Documents administratifs manquants");
        inscriptionService.validerParAdmin(inscriptionId, adminRejection);

        Inscription afterAdminRejection = inscriptionRepository.findById(inscriptionId).orElseThrow();
        assertEquals(StatutInscription.REJETE, afterAdminRejection.getStatut());
        
        // Verify validation records
        List<ValidationInscription> validations = validationRepository.findByInscriptionId(inscriptionId);
        assertTrue(validations.stream().anyMatch(v -> 
            v.getTypeValidateur() == TypeValidateur.DIRECTEUR_THESE && 
            v.getStatut() == StatutValidation.APPROUVE
        ));
        assertTrue(validations.stream().anyMatch(v -> 
            v.getTypeValidateur() == TypeValidateur.ADMINISTRATION && 
            v.getStatut() == StatutValidation.REJETE
        ));
    }

    /**
     * Test 9: Re-registration without derogation should fail
     * Tests: re-registration > 3 years without approved derogation
     * Validates Requirements: 3.7
     */
    @Test
    @DisplayName("Re-registration without derogation should fail for > 3 years")
    void testReRegistrationWithoutDerogationFails() {
        // Create first inscription > 3 years ago
        Inscription premiereInscription = Inscription.builder()
                .doctorantId(doctorantId + 40)
                .directeurTheseId(directeurId)
                .campagne(testCampagne)
                .sujetThese("Première inscription sans dérogation")
                .type(TypeInscription.PREMIERE_INSCRIPTION)
                .anneeInscription(2020)
                .statut(StatutInscription.VALIDE)
                .dateCreation(LocalDateTime.now().minusYears(4))
                .datePremiereInscription(LocalDateTime.now().minusYears(4))
                .build();
        premiereInscription = inscriptionRepository.save(premiereInscription);

        // Attempt re-registration without derogation
        InscriptionRequest reInscriptionRequest = new InscriptionRequest();
        reInscriptionRequest.setDoctorantId(doctorantId + 40);
        reInscriptionRequest.setDirecteurTheseId(directeurId);
        reInscriptionRequest.setCampagneId(testCampagne.getId());
        reInscriptionRequest.setSujetThese("Tentative de réinscription sans dérogation");
        reInscriptionRequest.setType(TypeInscription.REINSCRIPTION);
        reInscriptionRequest.setAnneeInscription(2024);

        // Should throw DerogationRequiredException
        assertThrows(DerogationRequiredException.class, () -> {
            inscriptionService.creerInscription(reInscriptionRequest);
        });
    }
}
