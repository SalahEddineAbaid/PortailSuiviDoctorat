package ma.emsi.inscriptionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.InscriptionRequest;
import ma.emsi.inscriptionservice.DTOs.ValidationRequest;
import ma.emsi.inscriptionservice.DTOs.*;
import ma.emsi.inscriptionservice.entities.*;
import ma.emsi.inscriptionservice.enums.*;
import ma.emsi.inscriptionservice.exceptions.DerogationRequiredException;
import ma.emsi.inscriptionservice.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final CampagneRepository campagneRepository;
    private final InfosDoctorantRepository infosDoctorantRepository;
    private final InfosTheseRepository infosTheseRepository;
    private final ValidationInscriptionRepository validationRepository;
    private final DocumentInscriptionRepository documentRepository;
    private final NotificationService notificationService;
    private final DocumentGenereRepository documentGenereRepository;
    private final DerogationService derogationService;
    private final AlerteService alerteService;
    private final DashboardService dashboardService;

    /**
     * Créer une nouvelle demande d'inscription
     */
    @Transactional
    public InscriptionResponse creerInscription(InscriptionRequest request) {
        log.info("Création d'une nouvelle inscription pour le doctorant {}", request.getDoctorantId());

        // Vérifier que la campagne est ouverte
        Campagne campagne = campagneRepository.findById(request.getCampagneId())
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));

        if (!campagne.isOuverte()) {
            throw new RuntimeException("La campagne d'inscription n'est pas ouverte");
        }

        // Vérifier qu'il n'y a pas déjà une inscription pour cette année
        inscriptionRepository.findInscriptionByDoctorantAndAnnee(
                request.getDoctorantId(),
                request.getType(),
                request.getAnneeInscription()).ifPresent(i -> {
                    throw new RuntimeException("Une inscription existe déjà pour cette année");
                });

        // Récupérer la première inscription si c'est une réinscription
        LocalDateTime datePremiereInscription = null;
        if (request.getType() == TypeInscription.REINSCRIPTION) {
            datePremiereInscription = inscriptionRepository
                    .findPremiereInscriptionByDoctorant(request.getDoctorantId())
                    .map(Inscription::getDateCreation)
                    .orElseThrow(() -> new RuntimeException("Première inscription introuvable"));

            // Vérifier la durée
            long duree = java.time.temporal.ChronoUnit.YEARS.between(
                    datePremiereInscription, LocalDateTime.now());

            if (duree >= 6) {
                throw new RuntimeException("Durée maximale de 6 ans dépassée");
            }

            // Requirement 3.7: Check if duration > 3 years and derogation is required
            if (duree >= 3) {
                log.warn("Dépassement de 3 ans pour le doctorant {}, vérification de la dérogation",
                        request.getDoctorantId());

                // Calculate precise duration in years (including fractional years)
                double dureeExacte = java.time.temporal.ChronoUnit.DAYS.between(
                        datePremiereInscription, LocalDateTime.now()) / 365.25;

                // Check if approved derogation exists
                Inscription premiereInscription = inscriptionRepository
                        .findPremiereInscriptionByDoctorant(request.getDoctorantId())
                        .orElseThrow(() -> new RuntimeException("Première inscription introuvable"));

                boolean hasApprovedDerogation = derogationService
                        .getDerogation(premiereInscription.getId())
                        .map(d -> d.getStatut() == ma.emsi.inscriptionservice.enums.StatutDerogation.APPROUVE_PED)
                        .orElse(false);

                if (!hasApprovedDerogation) {
                    log.error("Tentative de réinscription sans dérogation approuvée pour le doctorant {} " +
                            "(durée: {} ans)", request.getDoctorantId(), dureeExacte);
                    throw new DerogationRequiredException(
                            premiereInscription.getId(),
                            request.getDoctorantId(),
                            dureeExacte);
                }

                log.info("Dérogation approuvée trouvée pour le doctorant {}, réinscription autorisée",
                        request.getDoctorantId());
            }
        }

        // Créer l'inscription
        Inscription inscription = Inscription.builder()
                .doctorantId(request.getDoctorantId())
                .directeurTheseId(request.getDirecteurTheseId())
                .campagne(campagne)
                .sujetThese(request.getSujetThese())
                .type(request.getType())
                .anneeInscription(request.getAnneeInscription())
                .statut(StatutInscription.BROUILLON)
                .datePremiereInscription(datePremiereInscription)
                .build();

        inscription = inscriptionRepository.save(inscription);

        // Créer les informations du doctorant
        InfosDoctorant infosDoctorant = InfosDoctorant.builder()
                .inscription(inscription)
                .cin(request.getCin())
                .cne(request.getCne())
                .telephone(request.getTelephone())
                .adresse(request.getAdresse())
                .ville(request.getVille())
                .pays(request.getPays())
                .dateNaissance(request.getDateNaissance())
                .lieuNaissance(request.getLieuNaissance())
                .nationalite(request.getNationalite())
                .build();

        infosDoctorantRepository.save(infosDoctorant);

        // Créer les informations de la thèse
        InfosThese infosThese = InfosThese.builder()
                .inscription(inscription)
                .titreThese(request.getTitreThese())
                .discipline(request.getDiscipline())
                .laboratoire(request.getLaboratoire())
                .etablissementAccueil(request.getEtablissementAccueil())
                .cotutelle(request.getCotutelle())
                .universitePartenaire(request.getUniversitePartenaire())
                .paysPartenaire(request.getPaysPartenaire())
                .dateDebutPrevue(request.getDateDebutPrevue())
                .build();

        infosTheseRepository.save(infosThese);

        // Requirement 4.6: Verify and generate alerts for re-registration
        if (request.getType() == TypeInscription.REINSCRIPTION) {
            log.info("Vérification des alertes de durée pour la réinscription {}", inscription.getId());
            alerteService.verifierEtGenererAlertes(inscription);
        }

        log.info("Inscription créée avec succès: ID {}", inscription.getId());

        return mapToResponse(inscription);
    }

    /**
     * Soumettre l'inscription pour validation
     */
    @Transactional
    public InscriptionResponse soumettre(Long inscriptionId, Long doctorantId) {
        log.info("Soumission de l'inscription {} par le doctorant {}", inscriptionId, doctorantId);

        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        // Vérifier que c'est bien l'inscription du doctorant
        if (!inscription.getDoctorantId().equals(doctorantId)) {
            throw new RuntimeException("Accès non autorisé");
        }

        // Vérifier l'état
        if (inscription.getStatut() != StatutInscription.BROUILLON) {
            throw new RuntimeException("L'inscription a déjà été soumise");
        }

        // Vérifier que tous les documents obligatoires sont présents
        verifierDocumentsObligatoires(inscriptionId);

        // Requirement 4.6: Verify and generate alerts when submitting re-registration
        if (inscription.getType() == TypeInscription.REINSCRIPTION) {
            log.info("Vérification des alertes de durée lors de la soumission de l'inscription {}",
                    inscriptionId);
            alerteService.verifierEtGenererAlertes(inscription);
        }

        // Changer le statut
        inscription.setStatut(StatutInscription.EN_ATTENTE_DIRECTEUR);
        inscription = inscriptionRepository.save(inscription);

        // Créer la validation pour le directeur
        ValidationInscription validation = ValidationInscription.builder()
                .inscription(inscription)
                .validateurId(inscription.getDirecteurTheseId())
                .typeValidateur(TypeValidateur.DIRECTEUR_THESE)
                .statut(StatutValidation.EN_ATTENTE)
                .build();

        validationRepository.save(validation);

        // Envoyer notification au directeur
        notificationService.notifierDirecteurNouvelleDemande(
                inscription.getDirecteurTheseId(),
                inscriptionId);

        // Requirement 7.1: Publish INSCRIPTION_SOUMISE event
        notificationService.publierEvenementInscriptionSoumise(
                inscriptionId,
                inscription.getDoctorantId(),
                inscription.getDirecteurTheseId());

        log.info("Inscription {} soumise avec succès", inscriptionId);

        return mapToResponse(inscription);
    }

    /**
     * Valider l'inscription par le directeur de thèse
     */
    @Transactional
    public InscriptionResponse validerParDirecteur(Long inscriptionId, ValidationRequest request,
            Long directeurId) {
        log.info("Validation de l'inscription {} par le directeur {}", inscriptionId, directeurId);

        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        // Vérifier les droits
        if (!inscription.getDirecteurTheseId().equals(directeurId)) {
            throw new RuntimeException("Accès non autorisé");
        }

        // Vérifier l'état
        if (inscription.getStatut() != StatutInscription.EN_ATTENTE_DIRECTEUR) {
            throw new RuntimeException("L'inscription n'est pas en attente de validation");
        }

        // Récupérer la validation
        ValidationInscription validation = validationRepository
                .findByInscriptionIdAndTypeValidateur(inscriptionId, TypeValidateur.DIRECTEUR_THESE)
                .orElseThrow(() -> new RuntimeException("Validation introuvable"));

        // Déterminer le statut basé sur approuve
        StatutValidation statutValidation = Boolean.TRUE.equals(request.getApprouve())
                ? StatutValidation.APPROUVE
                : StatutValidation.REJETE;

        // Mettre à jour la validation
        validation.setStatut(statutValidation);
        validation.setCommentaire(request.getCommentaire());
        validation.setDateValidation(LocalDateTime.now());
        validationRepository.save(validation);

        // Mettre à jour l'inscription
        inscription.setCommentaireDirecteur(request.getCommentaire());

        if (statutValidation == StatutValidation.APPROUVE) {
            inscription.setStatut(StatutInscription.EN_ATTENTE_ADMIN);

            // Créer la validation pour l'administration
            ValidationInscription validationAdmin = ValidationInscription.builder()
                    .inscription(inscription)
                    .validateurId(0L) // Administration
                    .typeValidateur(TypeValidateur.ADMINISTRATION)
                    .statut(StatutValidation.EN_ATTENTE)
                    .build();

            validationRepository.save(validationAdmin);

            // Notifier l'administration
            notificationService.notifierAdminNouvelleDemande(inscriptionId);

            // Requirement 7.2: Publish INSCRIPTION_VALIDEE_DIRECTEUR event
            notificationService.publierEvenementInscriptionValideeDirecteur(
                    inscriptionId,
                    directeurId);
        } else {
            inscription.setStatut(StatutInscription.REJETE);

            // Notifier le doctorant du rejet
            notificationService.notifierDoctorantRejet(inscription.getDoctorantId(), inscriptionId);

            // Requirement 7.3: Publish INSCRIPTION_REJETEE_DIRECTEUR event
            notificationService.publierEvenementInscriptionRejeteeDirecteur(
                    inscriptionId,
                    directeurId,
                    request.getCommentaire());
        }

        inscription = inscriptionRepository.save(inscription);

        log.info("Inscription {} validée par le directeur: {}", inscriptionId, statutValidation);

        return mapToResponse(inscription);
    }

    /**
     * Valider l'inscription par l'administration
     */
    @Transactional
    public InscriptionResponse validerParAdmin(Long inscriptionId, ValidationRequest request) {
        log.info("Validation administrative de l'inscription {}", inscriptionId);

        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        // Vérifier l'état
        if (inscription.getStatut() != StatutInscription.EN_ATTENTE_ADMIN) {
            throw new RuntimeException("L'inscription n'est pas en attente de validation administrative");
        }

        // Récupérer la validation
        ValidationInscription validation = validationRepository
                .findByInscriptionIdAndTypeValidateur(inscriptionId, TypeValidateur.ADMINISTRATION)
                .orElseThrow(() -> new RuntimeException("Validation introuvable"));

        // Vérifier la durée si c'est une réinscription
        if (inscription.getType() == TypeInscription.REINSCRIPTION) {
            long duree = inscription.calculerDuree();

            if (duree >= 3 && !Boolean.TRUE.equals(request.getAccordeDerogation())) {
                throw new RuntimeException("Dérogation requise pour un dépassement de 3 ans");
            }

            if (Boolean.TRUE.equals(request.getAccordeDerogation())) {
                inscription.setDerogation(true);
                inscription.setMotifDerogation(request.getMotifDerogation());
            }
        }

        // Déterminer le statut basé sur approuve
        StatutValidation statutValidation = Boolean.TRUE.equals(request.getApprouve())
                ? StatutValidation.APPROUVE
                : StatutValidation.REJETE;

        // Mettre à jour la validation
        validation.setStatut(statutValidation);
        validation.setCommentaire(request.getCommentaire());
        validation.setDateValidation(LocalDateTime.now());
        validationRepository.save(validation);

        // Mettre à jour l'inscription
        inscription.setCommentaireAdmin(request.getCommentaire());

        if (statutValidation == StatutValidation.APPROUVE) {
            inscription.setStatut(StatutInscription.VALIDE);
            inscription.setDateValidation(LocalDateTime.now());

            // Si c'est la première inscription, mettre à jour la date
            if (inscription.getType() == TypeInscription.PREMIERE_INSCRIPTION) {
                inscription.setDatePremiereInscription(inscription.getDateCreation());
            }

            // Générer l'attestation d'inscription
            notificationService.genererAttestationInscription(inscriptionId);

            // Notifier le doctorant et le directeur
            notificationService.notifierValidationDefinitive(
                    inscription.getDoctorantId(),
                    inscription.getDirecteurTheseId(),
                    inscriptionId);

            // Requirement 7.4: Publish INSCRIPTION_VALIDEE_ADMIN event
            notificationService.publierEvenementInscriptionValideeAdmin(inscriptionId);
        } else {
            inscription.setStatut(StatutInscription.REJETE);

            // Notifier le doctorant du rejet
            notificationService.notifierDoctorantRejet(inscription.getDoctorantId(), inscriptionId);

            // Requirement 7.5: Publish INSCRIPTION_REJETEE_ADMIN event
            notificationService.publierEvenementInscriptionRejeteeAdmin(
                    inscriptionId,
                    request.getCommentaire());
        }

        inscription = inscriptionRepository.save(inscription);

        log.info("Inscription {} validée administrativement: {}", inscriptionId, statutValidation);

        return mapToResponse(inscription);
    }

    /**
     * Récupérer une inscription par ID
     */
    public InscriptionResponse getInscription(Long id) {
        Inscription inscription = inscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));
        return mapToResponse(inscription);
    }

    /**
     * Récupérer les inscriptions d'un doctorant
     */
    public List<InscriptionResponse> getInscriptionsDoctorant(Long doctorantId) {
        return inscriptionRepository.findByDoctorantId(doctorantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les inscriptions en attente pour un directeur
     */
    public List<InscriptionResponse> getInscriptionsEnAttenteDirecteur(Long directeurId) {
        return inscriptionRepository.findInscriptionsEnAttenteValidation(directeurId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer toutes les inscriptions en attente admin
     */
    public List<InscriptionResponse> getInscriptionsEnAttenteAdmin() {
        return inscriptionRepository.findByStatut(StatutInscription.EN_ATTENTE_ADMIN)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer l'attestation d'inscription
     * Requirements: 2.6, 2.7
     */
    public byte[] getAttestation(Long inscriptionId, Long userId, String role) {
        log.info("Récupération de l'attestation pour l'inscription {} par l'utilisateur {} (role: {})",
                inscriptionId, userId, role);

        // Normalize role (handle both "DOCTORANT" and "ROLE_DOCTORANT" formats)
        String normalizedRole = role != null ? role.replace("ROLE_", "") : "";

        // Récupérer l'inscription
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        // Vérifier l'autorisation (Requirements 2.6)
        boolean isAuthorized = false;

        if ("DOCTORANT".equals(normalizedRole) && inscription.getDoctorantId().equals(userId)) {
            isAuthorized = true;
        } else if ("DIRECTEUR".equals(normalizedRole) && inscription.getDirecteurTheseId().equals(userId)) {
            isAuthorized = true;
        } else if ("ADMIN".equals(normalizedRole)) {
            isAuthorized = true;
        }

        if (!isAuthorized) {
            throw new RuntimeException("Accès non autorisé à cette attestation");
        }

        // Récupérer le document généré
        DocumentGenere documentGenere = documentGenereRepository
                .findByInscriptionIdAndType(inscriptionId, TypeDocumentGenere.ATTESTATION_INSCRIPTION)
                .orElseThrow(() -> new RuntimeException("Attestation non trouvée pour cette inscription"));

        // Lire le fichier depuis le système de fichiers
        try {
            Path filePath = Paths.get(documentGenere.getCheminFichier());

            if (!Files.exists(filePath)) {
                throw new RuntimeException("Le fichier d'attestation n'existe pas sur le disque");
            }

            byte[] pdfContent = Files.readAllBytes(filePath);

            log.info("Attestation récupérée avec succès pour l'inscription {}", inscriptionId);

            return pdfContent;
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du fichier d'attestation: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la récupération de l'attestation: " + e.getMessage());
        }
    }

    // Méthodes utilitaires

    /**
     * Récupère les inscriptions par liste de statuts
     * 
     * @param statuts Liste des statuts à rechercher
     * @return Liste des inscriptions correspondantes
     */
    public List<Inscription> getInscriptionsByStatuts(List<StatutInscription> statuts) {
        return inscriptionRepository.findByStatutIn(statuts);
    }

    /**
     * Vérifie les alertes de durée pour toutes les inscriptions en batch
     * 
     * Requirements: 4.1, 4.2, 4.3
     * 
     * @param inscriptions Liste des inscriptions à vérifier
     * @return Résumé de la vérification
     */
    @Transactional
    public ma.emsi.inscriptionservice.DTOs.AlerteVerificationSummary verifierAlertesEnBatch(
            List<Inscription> inscriptions) {
        log.info("Vérification des alertes en batch pour {} inscriptions", inscriptions.size());
        return alerteService.verifierAlertesEnBatch(inscriptions);
    }

    /**
     * Récupère le tableau de bord complet d'un doctorant
     * 
     * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
     * 
     * @param doctorantId ID du doctorant
     * @return Dashboard complet avec toutes les informations
     */
    public ma.emsi.inscriptionservice.DTOs.DashboardResponse getDashboardDoctorant(Long doctorantId) {
        log.info("Récupération du dashboard pour le doctorant {}", doctorantId);
        return dashboardService.getDashboardDoctorant(doctorantId);
    }

    /**
     * Vérifie si un directeur est le directeur de thèse d'un doctorant
     * 
     * @param directeurId ID du directeur
     * @param doctorantId ID du doctorant
     * @return true si le directeur est le directeur de thèse du doctorant
     */
    public boolean isDirecteurOfDoctorant(Long directeurId, Long doctorantId) {
        log.debug("Vérification si le directeur {} est le directeur du doctorant {}",
                directeurId, doctorantId);

        // Check if there's any inscription where this director is the thesis director
        List<Inscription> inscriptions = inscriptionRepository.findByDoctorantId(doctorantId);

        return inscriptions.stream()
                .anyMatch(inscription -> inscription.getDirecteurTheseId().equals(directeurId));
    }

    private void verifierDocumentsObligatoires(Long inscriptionId) {
        List<TypeDocument> documentsObligatoires = List.of(
                TypeDocument.DIPLOME_MASTER,
                TypeDocument.CV,
                TypeDocument.LETTRE_MOTIVATION,
                TypeDocument.RELEVE_NOTES);

        for (TypeDocument type : documentsObligatoires) {
            if (!documentRepository.existsByInscriptionIdAndTypeDocument(inscriptionId, type)) {
                throw new RuntimeException("Document manquant: " + type);
            }
        }
    }

    private InscriptionResponse mapToResponse(Inscription inscription) {
        return InscriptionResponse.builder()
                .id(inscription.getId())
                .doctorantId(inscription.getDoctorantId())
                .directeurTheseId(inscription.getDirecteurTheseId())
                .sujetThese(inscription.getSujetThese())
                .type(inscription.getType())
                .anneeInscription(inscription.getAnneeInscription())
                .statut(inscription.getStatut())
                .dateCreation(inscription.getDateCreation())
                .dateValidation(inscription.getDateValidation())
                .dureeDoctorat(inscription.calculerDuree())
                .derogation(inscription.getDerogation())
                .motifDerogation(inscription.getMotifDerogation())
                .commentaireDirecteur(inscription.getCommentaireDirecteur())
                .commentaireAdmin(inscription.getCommentaireAdmin())
                .infosDoctorant(mapInfosDoctorant(inscription.getInfosDoctorant()))
                .infosThese(mapInfosThese(inscription.getInfosThese()))
                .documents(mapDocuments(inscription.getDocuments()))
                .validations(mapValidations(inscription.getValidations()))
                .build();
    }

    private InfosDoctorantResponse mapInfosDoctorant(InfosDoctorant infos) {
        if (infos == null)
            return null;
        return InfosDoctorantResponse.builder()
                .id(infos.getId())
                .cin(infos.getCin())
                .cne(infos.getCne())
                .telephone(infos.getTelephone())
                .adresse(infos.getAdresse())
                .ville(infos.getVille())
                .pays(infos.getPays())
                .dateNaissance(infos.getDateNaissance())
                .lieuNaissance(infos.getLieuNaissance())
                .nationalite(infos.getNationalite())
                .build();
    }

    private InfosTheseResponse mapInfosThese(InfosThese infos) {
        if (infos == null)
            return null;
        return InfosTheseResponse.builder()
                .id(infos.getId())
                .titreThese(infos.getTitreThese())
                .discipline(infos.getDiscipline())
                .laboratoire(infos.getLaboratoire())
                .etablissementAccueil(infos.getEtablissementAccueil())
                .cotutelle(infos.getCotutelle())
                .universitePartenaire(infos.getUniversitePartenaire())
                .paysPartenaire(infos.getPaysPartenaire())
                .dateDebutPrevue(infos.getDateDebutPrevue())
                .build();
    }

    private List<DocumentResponse> mapDocuments(List<DocumentInscription> documents) {
        if (documents == null)
            return List.of();
        return documents.stream()
                .map(doc -> DocumentResponse.builder()
                        .id(doc.getId())
                        .typeDocument(doc.getTypeDocument())
                        .nomFichier(doc.getNomFichier())
                        .tailleFichier(doc.getTailleFichier())
                        .mimeType(doc.getMimeType())
                        .dateUpload(doc.getDateUpload())
                        .valide(doc.getValide())
                        .commentaire(doc.getCommentaire())
                        .build())
                .collect(Collectors.toList());
    }

    private List<ValidationResponse> mapValidations(List<ValidationInscription> validations) {
        if (validations == null)
            return List.of();
        return validations.stream()
                .map(val -> ValidationResponse.builder()
                        .id(val.getId())
                        .validateurId(val.getValidateurId())
                        .typeValidateur(val.getTypeValidateur())
                        .statut(val.getStatut())
                        .commentaire(val.getCommentaire())
                        .dateValidation(val.getDateValidation())
                        .dateCreation(val.getDateCreation())
                        .build())
                .collect(Collectors.toList());
    }
}
