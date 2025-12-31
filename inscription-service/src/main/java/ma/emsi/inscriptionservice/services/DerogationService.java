package ma.emsi.inscriptionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.NotificationDTO;
import ma.emsi.inscriptionservice.client.UserServiceClient;
import ma.emsi.inscriptionservice.DTOs.UserDTO;
import ma.emsi.inscriptionservice.entities.DerogationRequest;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.StatutDerogation;
import ma.emsi.inscriptionservice.repositories.DerogationRequestRepository;
import ma.emsi.inscriptionservice.repositories.InscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Service for managing derogation requests for students exceeding the standard 3-year period.
 * Implements the complete derogation workflow: student request → director approval → PED approval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DerogationService {

    private final DerogationRequestRepository derogationRequestRepository;
    private final InscriptionRepository inscriptionRepository;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, NotificationDTO> kafkaTemplate;

    @Value("${kafka.topic.notifications:notifications}")
    private String notificationTopic;

    /**
     * Create a new derogation request for an inscription exceeding 3 years.
     * 
     * @param inscriptionId The inscription ID
     * @param motif The reason for requesting derogation
     * @param documents Supporting documents (optional)
     * @return The created derogation request
     * @throws RuntimeException if inscription not found or derogation already exists
     */
    @Transactional
    public DerogationRequest creerDerogation(Long inscriptionId, String motif, byte[] documents) {
        log.info("Création d'une demande de dérogation pour l'inscription {}", inscriptionId);

        // Validate inscription exists
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        // Check if derogation already exists
        Optional<DerogationRequest> existingDerogation = 
                derogationRequestRepository.findByInscriptionId(inscriptionId);
        
        if (existingDerogation.isPresent()) {
            DerogationRequest existing = existingDerogation.get();
            if (existing.getStatut() != StatutDerogation.REJETE) {
                throw new RuntimeException("Une demande de dérogation existe déjà pour cette inscription");
            }
        }

        // Validate motif
        if (motif == null || motif.trim().isEmpty()) {
            throw new RuntimeException("Le motif de la dérogation est obligatoire");
        }

        // Create derogation request
        DerogationRequest derogation = DerogationRequest.builder()
                .inscription(inscription)
                .motif(motif)
                .documentsJustificatifs(documents)
                .statut(StatutDerogation.EN_ATTENTE)
                .dateDemande(LocalDateTime.now())
                .build();

        derogation = derogationRequestRepository.save(derogation);
        log.info("Demande de dérogation créée avec succès: {}", derogation.getId());

        // Send notification to director via Kafka
        notifierDirecteurDerogation(inscription, derogation);

        return derogation;
    }

    /**
     * Director validates or rejects a derogation request.
     * 
     * @param derogationId The derogation request ID
     * @param approuve True to approve, false to reject
     * @param commentaire Director's comment
     * @return The updated derogation request
     * @throws RuntimeException if derogation not found or invalid state
     */
    @Transactional
    public DerogationRequest validerParDirecteur(Long derogationId, boolean approuve, String commentaire) {
        log.info("Validation directeur de la dérogation {} - approuvé: {}", derogationId, approuve);

        DerogationRequest derogation = derogationRequestRepository.findById(derogationId)
                .orElseThrow(() -> new RuntimeException("Demande de dérogation introuvable"));

        // Validate current status
        if (derogation.getStatut() != StatutDerogation.EN_ATTENTE) {
            throw new RuntimeException("Cette demande de dérogation ne peut plus être modifiée");
        }

        // Update derogation
        derogation.setCommentaireValidation(commentaire);
        derogation.setDateValidation(LocalDateTime.now());

        if (approuve) {
            derogation.setStatut(StatutDerogation.APPROUVE_DIRECTEUR);
            log.info("Dérogation {} approuvée par le directeur", derogationId);
            
            // Send notification to PED administrators
            notifierPEDDerogation(derogation);
        } else {
            derogation.setStatut(StatutDerogation.REJETE);
            log.info("Dérogation {} rejetée par le directeur", derogationId);
            
            // Send notification to student
            notifierDoctorantRejetDerogation(derogation);
        }

        return derogationRequestRepository.save(derogation);
    }

    /**
     * PED validates or rejects a derogation request.
     * 
     * @param derogationId The derogation request ID
     * @param approuve True to approve, false to reject
     * @param commentaire PED's comment
     * @return The updated derogation request
     * @throws RuntimeException if derogation not found or invalid state
     */
    @Transactional
    public DerogationRequest validerParPED(Long derogationId, boolean approuve, String commentaire) {
        log.info("Validation PED de la dérogation {} - approuvé: {}", derogationId, approuve);

        DerogationRequest derogation = derogationRequestRepository.findById(derogationId)
                .orElseThrow(() -> new RuntimeException("Demande de dérogation introuvable"));

        // Validate current status
        if (derogation.getStatut() != StatutDerogation.APPROUVE_DIRECTEUR) {
            throw new RuntimeException("Cette demande doit d'abord être approuvée par le directeur");
        }

        // Update derogation
        derogation.setCommentaireValidation(commentaire);
        derogation.setDateValidation(LocalDateTime.now());

        Inscription inscription = derogation.getInscription();

        if (approuve) {
            derogation.setStatut(StatutDerogation.APPROUVE_PED);
            
            // Set derogation flag on inscription
            inscription.setDerogation(true);
            inscription.setMotifDerogation(derogation.getMotif());
            inscriptionRepository.save(inscription);
            
            log.info("Dérogation {} approuvée par PED - inscription {} mise à jour", 
                    derogationId, inscription.getId());
            
            // Send notification to student
            notifierDoctorantApprobationDerogation(derogation);
        } else {
            derogation.setStatut(StatutDerogation.REJETE);
            
            // Block re-registration
            inscription.setBloqueReInscription(true);
            inscriptionRepository.save(inscription);
            
            log.info("Dérogation {} rejetée par PED - réinscription bloquée pour inscription {}", 
                    derogationId, inscription.getId());
            
            // Send notification to student
            notifierDoctorantRejetDerogation(derogation);
        }

        return derogationRequestRepository.save(derogation);
    }

    /**
     * Get derogation request for an inscription.
     * 
     * @param inscriptionId The inscription ID
     * @return The derogation request if exists
     */
    public Optional<DerogationRequest> getDerogation(Long inscriptionId) {
        return derogationRequestRepository.findByInscriptionId(inscriptionId);
    }

    /**
     * Check if a derogation is required for an inscription.
     * 
     * @param inscription The inscription to check
     * @return True if derogation is required (duration > 3 years and no approved derogation)
     */
    public boolean isDerogationRequise(Inscription inscription) {
        if (inscription.getDatePremiereInscription() == null) {
            return false;
        }

        double duree = ChronoUnit.DAYS.between(
                inscription.getDatePremiereInscription(), 
                LocalDateTime.now()
        ) / 365.25;

        if (duree <= 3.0) {
            return false;
        }

        // Check if approved derogation exists
        Optional<DerogationRequest> derogation = 
                derogationRequestRepository.findApprovedDerogationByInscription(inscription.getId());

        return derogation.isEmpty();
    }


    /**
     * Send notification to director about new derogation request.
     */
    private void notifierDirecteurDerogation(Inscription inscription, DerogationRequest derogation) {
        try {
            UserDTO directeur = userServiceClient.getUserById(inscription.getDirecteurTheseId());
            UserDTO doctorant = userServiceClient.getUserById(inscription.getDoctorantId());

            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail(directeur.getEmail())
                    .destinataireNom(directeur.getFirstName() + " " + directeur.getLastName())
                    .sujet("Nouvelle demande de dérogation à valider")
                    .message(String.format(
                            "Bonjour %s,\n\n" +
                            "Une demande de dérogation a été soumise par %s %s pour l'inscription ID: %d.\n\n" +
                            "Motif: %s\n\n" +
                            "Veuillez examiner cette demande et donner votre avis.\n\n" +
                            "Cordialement,\nService Doctorat",
                            directeur.getFirstName(),
                            doctorant.getFirstName(),
                            doctorant.getLastName(),
                            inscription.getId(),
                            derogation.getMotif()
                    ))
                    .type(NotificationDTO.TypeNotification.DEROGATION_DEMANDEE)
                    .inscriptionId(inscription.getId())
                    .dateEnvoi(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(notificationTopic, notification);
            log.info("Notification de dérogation envoyée au directeur {} via Kafka", 
                    inscription.getDirecteurTheseId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de notification au directeur: {}", e.getMessage());
        }
    }

    /**
     * Send notification to PED administrators about director-approved derogation.
     */
    private void notifierPEDDerogation(DerogationRequest derogation) {
        try {
            Inscription inscription = derogation.getInscription();
            UserDTO doctorant = userServiceClient.getUserById(inscription.getDoctorantId());

            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail("ped@doctorat.ma")
                    .destinataireNom("Pôle Études Doctorales")
                    .sujet("Demande de dérogation approuvée par le directeur")
                    .message(String.format(
                            "Bonjour,\n\n" +
                            "Une demande de dérogation pour %s %s (Inscription ID: %d) a été approuvée par le directeur de thèse.\n\n" +
                            "Motif: %s\n\n" +
                            "Commentaire du directeur: %s\n\n" +
                            "Veuillez examiner cette demande pour validation finale.\n\n" +
                            "Cordialement,\nService Doctorat",
                            doctorant.getFirstName(),
                            doctorant.getLastName(),
                            inscription.getId(),
                            derogation.getMotif(),
                            derogation.getCommentaireValidation() != null ? derogation.getCommentaireValidation() : "Aucun"
                    ))
                    .type(NotificationDTO.TypeNotification.DEROGATION_APPROUVEE_DIRECTEUR)
                    .inscriptionId(inscription.getId())
                    .dateEnvoi(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(notificationTopic, notification);
            log.info("Notification de dérogation envoyée au PED via Kafka");
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de notification au PED: {}", e.getMessage());
        }
    }

    /**
     * Send notification to student about derogation rejection.
     */
    private void notifierDoctorantRejetDerogation(DerogationRequest derogation) {
        try {
            Inscription inscription = derogation.getInscription();
            UserDTO doctorant = userServiceClient.getUserById(inscription.getDoctorantId());

            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail(doctorant.getEmail())
                    .destinataireNom(doctorant.getFirstName() + " " + doctorant.getLastName())
                    .sujet("Demande de dérogation rejetée")
                    .message(String.format(
                            "Bonjour %s,\n\n" +
                            "Votre demande de dérogation pour l'inscription ID: %d a été rejetée.\n\n" +
                            "Commentaire: %s\n\n" +
                            "Pour plus d'informations, veuillez contacter le service des études doctorales.\n\n" +
                            "Cordialement,\nService Doctorat",
                            doctorant.getFirstName(),
                            inscription.getId(),
                            derogation.getCommentaireValidation() != null ? derogation.getCommentaireValidation() : "Aucun commentaire"
                    ))
                    .type(NotificationDTO.TypeNotification.DEROGATION_REJETEE)
                    .inscriptionId(inscription.getId())
                    .dateEnvoi(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(notificationTopic, notification);
            log.info("Notification de rejet de dérogation envoyée au doctorant {} via Kafka", 
                    inscription.getDoctorantId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de notification au doctorant: {}", e.getMessage());
        }
    }

    /**
     * Send notification to student about derogation approval.
     */
    private void notifierDoctorantApprobationDerogation(DerogationRequest derogation) {
        try {
            Inscription inscription = derogation.getInscription();
            UserDTO doctorant = userServiceClient.getUserById(inscription.getDoctorantId());

            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail(doctorant.getEmail())
                    .destinataireNom(doctorant.getFirstName() + " " + doctorant.getLastName())
                    .sujet("Demande de dérogation approuvée")
                    .message(String.format(
                            "Bonjour %s,\n\n" +
                            "Félicitations ! Votre demande de dérogation pour l'inscription ID: %d a été approuvée.\n\n" +
                            "Vous pouvez maintenant procéder à votre réinscription.\n\n" +
                            "Commentaire: %s\n\n" +
                            "Cordialement,\nService Doctorat",
                            doctorant.getFirstName(),
                            inscription.getId(),
                            derogation.getCommentaireValidation() != null ? derogation.getCommentaireValidation() : "Aucun commentaire"
                    ))
                    .type(NotificationDTO.TypeNotification.DEROGATION_APPROUVEE)
                    .inscriptionId(inscription.getId())
                    .dateEnvoi(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(notificationTopic, notification);
            log.info("Notification d'approbation de dérogation envoyée au doctorant {} via Kafka", 
                    inscription.getDoctorantId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de notification au doctorant: {}", e.getMessage());
        }
    }
}
