package ma.emsi.inscriptionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.*;
import ma.emsi.inscriptionservice.client.UserServiceClient;
import ma.emsi.inscriptionservice.entities.AlerteDuree;
import ma.emsi.inscriptionservice.entities.DocumentInscription;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.enums.TypeAlerte;
import ma.emsi.inscriptionservice.enums.TypeDocument;
import ma.emsi.inscriptionservice.repositories.AlerteDureeRepository;
import ma.emsi.inscriptionservice.repositories.InscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final InscriptionRepository inscriptionRepository;
    private final AlerteDureeRepository alerteDureeRepository;
    private final UserServiceClient userServiceClient;

    /**
     * Get comprehensive dashboard data for a doctoral student
     * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardDoctorant(Long doctorantId) {
        log.info("Fetching dashboard for doctorant: {}", doctorantId);

        // Fetch user information using dedicated student info method
        UserDTO user = userServiceClient.getStudentInfo(doctorantId);
        DoctorantInfo doctorantInfo = DoctorantInfo.builder()
                .id(user.getId())
                .nom(user.getLastName())
                .prenom(user.getFirstName())
                .email(user.getEmail())
                .build();

        // Fetch all inscriptions for the student
        List<Inscription> inscriptions = inscriptionRepository.findByDoctorantId(doctorantId);
        
        if (inscriptions.isEmpty()) {
            log.warn("No inscriptions found for doctorant: {}", doctorantId);
            return DashboardResponse.builder()
                    .doctorant(doctorantInfo)
                    .historiqueInscriptions(new ArrayList<>())
                    .alertes(new ArrayList<>())
                    .documentsManquants(new ArrayList<>())
                    .build();
        }

        // Sort inscriptions by year to get the most recent
        inscriptions.sort(Comparator.comparing(Inscription::getAnneeInscription).reversed());
        Inscription currentInscription = inscriptions.get(0);

        // Build current inscription details
        InscriptionCourante inscriptionCourante = buildInscriptionCourante(currentInscription);

        // Build inscription history
        List<InscriptionHistorique> historiqueInscriptions = buildHistoriqueInscriptions(inscriptions);

        // Fetch active alerts
        List<AlerteInfo> alertes = buildAlertes(doctorantId);

        // Get missing documents
        List<DocumentManquant> documentsManquants = getDocumentsManquants(currentInscription.getId());

        // Calculate statistics
        StatistiquesDossier statistiques = calculerStatistiques(currentInscription.getId());

        // Get next milestone
        Milestone prochaineMilestone = getProchaineMilestone(doctorantId);

        return DashboardResponse.builder()
                .doctorant(doctorantInfo)
                .inscriptionCourante(inscriptionCourante)
                .historiqueInscriptions(historiqueInscriptions)
                .alertes(alertes)
                .documentsManquants(documentsManquants)
                .prochaineMilestone(prochaineMilestone)
                .statistiques(statistiques)
                .build();
    }

    /**
     * Build current inscription details
     * Requirement: 5.1
     */
    private InscriptionCourante buildInscriptionCourante(Inscription inscription) {
        return InscriptionCourante.builder()
                .id(inscription.getId())
                .annee(inscription.getAnneeInscription())
                .type(inscription.getType())
                .statut(inscription.getStatut())
                .dureeDoctorat(inscription.calculerDuree())
                .derogationActive(inscription.getDerogation())
                .build();
    }

    /**
     * Build inscription history ordered by year
     * Requirement: 5.2
     */
    private List<InscriptionHistorique> buildHistoriqueInscriptions(List<Inscription> inscriptions) {
        return inscriptions.stream()
                .sorted(Comparator.comparing(Inscription::getAnneeInscription))
                .map(inscription -> InscriptionHistorique.builder()
                        .id(inscription.getId())
                        .annee(inscription.getAnneeInscription())
                        .type(inscription.getType())
                        .statut(inscription.getStatut())
                        .dateCreation(inscription.getDateCreation())
                        .dateValidation(inscription.getDateValidation())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Build active alerts with messages
     * Requirement: 5.3
     */
    private List<AlerteInfo> buildAlertes(Long doctorantId) {
        List<AlerteDuree> alertes = alerteDureeRepository.findAlertesActivesByDoctorant(doctorantId);
        
        return alertes.stream()
                .map(alerte -> AlerteInfo.builder()
                        .id(alerte.getId())
                        .type(alerte.getType())
                        .date(alerte.getDateAlerte())
                        .message(generateAlertMessage(alerte.getType()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Generate user-friendly alert messages
     */
    private String generateAlertMessage(TypeAlerte type) {
        return switch (type) {
            case APPROCHE_3_ANS -> "Vous approchez de la limite de 3 ans. Pensez à demander une dérogation si nécessaire.";
            case APPROCHE_6_ANS -> "Attention : Vous approchez de la limite maximale de 6 ans.";
            case DEPASSE_6_ANS -> "La durée maximale de 6 ans est dépassée. Réinscription bloquée.";
        };
    }

    /**
     * Get list of missing required documents
     * Requirement: 5.4
     */
    public List<DocumentManquant> getDocumentsManquants(Long inscriptionId) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription not found: " + inscriptionId));

        // Get all uploaded document types
        Set<TypeDocument> uploadedTypes = inscription.getDocuments().stream()
                .map(DocumentInscription::getTypeDocument)
                .collect(Collectors.toSet());

        // Define required documents based on inscription type
        List<TypeDocument> requiredDocuments = getRequiredDocuments(inscription);

        // Find missing documents
        return requiredDocuments.stream()
                .filter(type -> !uploadedTypes.contains(type))
                .map(type -> DocumentManquant.builder()
                        .type(type)
                        .libelle(getDocumentLibelle(type))
                        .obligatoire(true)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get required documents based on inscription type
     */
    private List<TypeDocument> getRequiredDocuments(Inscription inscription) {
        List<TypeDocument> required = new ArrayList<>();
        
        switch (inscription.getType()) {
            case PREMIERE_INSCRIPTION:
                required.add(TypeDocument.DIPLOME_MASTER);
                required.add(TypeDocument.CV);
                required.add(TypeDocument.LETTRE_MOTIVATION);
                required.add(TypeDocument.RELEVE_NOTES);
                required.add(TypeDocument.PROJET_THESE);
                required.add(TypeDocument.AUTORISATION_DIRECTEUR);
                break;
            case REINSCRIPTION:
                required.add(TypeDocument.CV);
                required.add(TypeDocument.AUTORISATION_DIRECTEUR);
                break;
        }
        
        return required;
    }

    /**
     * Get human-readable document label
     */
    private String getDocumentLibelle(TypeDocument type) {
        return switch (type) {
            case DIPLOME_MASTER -> "Diplôme de Master";
            case CV -> "Curriculum Vitae";
            case LETTRE_MOTIVATION -> "Lettre de Motivation";
            case RELEVE_NOTES -> "Relevé de Notes";
            case PROJET_THESE -> "Projet de Thèse";
            case AUTORISATION_DIRECTEUR -> "Autorisation du Directeur";
            case AUTRE -> "Autre Document";
        };
    }

    /**
     * Calculate dossier completion statistics
     * Requirement: 5.5
     */
    public StatistiquesDossier calculerStatistiques(Long inscriptionId) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription not found: " + inscriptionId));

        List<DocumentInscription> documents = inscription.getDocuments();
        int documentsTotal = getRequiredDocuments(inscription).size();
        int documentsValides = (int) documents.stream()
                .filter(doc -> doc.getValide() != null && doc.getValide())
                .count();

        double tauxCompletion = documentsTotal > 0 
                ? (documentsValides * 100.0) / documentsTotal 
                : 0.0;

        return StatistiquesDossier.builder()
                .tauxCompletionDossier(Math.round(tauxCompletion * 100.0) / 100.0)
                .documentsValides(documentsValides)
                .documentsTotal(documentsTotal)
                .build();
    }

    /**
     * Get next milestone for the student
     * Requirement: 5.6
     */
    public Milestone getProchaineMilestone(Long doctorantId) {
        List<Inscription> inscriptions = inscriptionRepository.findByDoctorantId(doctorantId);
        
        if (inscriptions.isEmpty()) {
            return null;
        }

        // Get most recent inscription
        inscriptions.sort(Comparator.comparing(Inscription::getAnneeInscription).reversed());
        Inscription currentInscription = inscriptions.get(0);

        // Determine next milestone based on status
        return switch (currentInscription.getStatut()) {
            case BROUILLON -> Milestone.builder()
                    .type("Soumission du dossier")
                    .dateEcheance(calculateCampagneEndDate(currentInscription))
                    .statut("EN_ATTENTE")
                    .build();
            case SOUMIS -> Milestone.builder()
                    .type("Validation du directeur")
                    .dateEcheance(LocalDate.now().plusDays(15))
                    .statut("EN_COURS")
                    .build();
            case EN_ATTENTE_DIRECTEUR -> Milestone.builder()
                    .type("Validation du directeur")
                    .dateEcheance(LocalDate.now().plusDays(15))
                    .statut("EN_COURS")
                    .build();
            case APPROUVE_DIRECTEUR -> Milestone.builder()
                    .type("Validation administrative")
                    .dateEcheance(LocalDate.now().plusDays(10))
                    .statut("EN_COURS")
                    .build();
            case EN_ATTENTE_ADMIN -> Milestone.builder()
                    .type("Validation administrative")
                    .dateEcheance(LocalDate.now().plusDays(10))
                    .statut("EN_COURS")
                    .build();
            case VALIDE -> Milestone.builder()
                    .type("Réinscription année suivante")
                    .dateEcheance(calculateNextYearDate(currentInscription))
                    .statut("PLANIFIE")
                    .build();
            case REJETE -> Milestone.builder()
                    .type("Correction du dossier")
                    .dateEcheance(calculateCampagneEndDate(currentInscription))
                    .statut("URGENT")
                    .build();
        };
    }

    /**
     * Calculate campaign end date
     */
    private LocalDate calculateCampagneEndDate(Inscription inscription) {
        if (inscription.getCampagne() != null && inscription.getCampagne().getDateFin() != null) {
            return inscription.getCampagne().getDateFin();
        }
        return LocalDate.now().plusMonths(1);
    }

    /**
     * Calculate next year registration date
     */
    private LocalDate calculateNextYearDate(Inscription inscription) {
        int currentYear = inscription.getAnneeInscription();
        return LocalDate.of(currentYear + 1, 9, 1); // September 1st of next year
    }
}
