package ma.emsi.inscriptionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.client.UserServiceClient;
import ma.emsi.inscriptionservice.DTOs.NotificationDTO;
import ma.emsi.inscriptionservice.DTOs.UserDTO;
import ma.emsi.inscriptionservice.entities.InfosDoctorant;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.repositories.InscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final KafkaTemplate<String, NotificationDTO> kafkaTemplate;
    private final UserServiceClient userServiceClient;
    private final AttestationPdfGenerator attestationPdfGenerator;
    private final InscriptionRepository inscriptionRepository;

    @Value("${kafka.topic.notifications:notifications}")
    private String notificationTopic;

    public void notifierDirecteurNouvelleDemande(Long directeurId, Long inscriptionId) {
        log.info("Notification directeur {} - nouvelle demande {}", directeurId, inscriptionId);
        
        try {
            UserDTO directeur = userServiceClient.getUserById(directeurId);
            
            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail(directeur.getEmail())
                    .destinataireNom(directeur.getFirstName() + " " + directeur.getLastName())
                    .sujet("Nouvelle demande d'inscription à valider")
                    .message(String.format(
                            "Bonjour %s,\n\nUne nouvelle demande d'inscription (ID: %d) nécessite votre validation.\n\nCordialement,\nService Doctorat",
                            directeur.getFirstName(), inscriptionId
                    ))
                    .type(NotificationDTO.TypeNotification.NOUVELLE_DEMANDE_DIRECTEUR)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Notification envoyée au directeur {} via Kafka", directeurId);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de notification au directeur {}: {}", directeurId, e.getMessage());
        }
    }

    public void notifierAdminNouvelleDemande(Long inscriptionId) {
        log.info("Notification admin - nouvelle demande {}", inscriptionId);
        
        try {
            // TODO: Récupérer l'email admin depuis la configuration
            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail("admin@doctorat.ma")
                    .destinataireNom("Administration")
                    .sujet("Nouvelle demande d'inscription à valider")
                    .message(String.format(
                            "Bonjour,\n\nUne nouvelle demande d'inscription (ID: %d) nécessite votre validation administrative.\n\nCordialement,\nService Doctorat",
                            inscriptionId
                    ))
                    .type(NotificationDTO.TypeNotification.NOUVELLE_DEMANDE_ADMIN)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Notification envoyée à l'administration via Kafka");
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de notification à l'admin: {}", e.getMessage());
        }
    }

    public void notifierDoctorantRejet(Long doctorantId, Long inscriptionId) {
        log.info("Notification doctorant {} - rejet demande {}", doctorantId, inscriptionId);
        
        try {
            UserDTO doctorant = userServiceClient.getUserById(doctorantId);
            
            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail(doctorant.getEmail())
                    .destinataireNom(doctorant.getFirstName() + " " + doctorant.getLastName())
                    .sujet("Demande d'inscription rejetée")
                    .message(String.format(
                            "Bonjour %s,\n\nVotre demande d'inscription (ID: %d) a été rejetée.\n\nVeuillez consulter les commentaires pour plus de détails.\n\nCordialement,\nService Doctorat",
                            doctorant.getFirstName(), inscriptionId
                    ))
                    .type(NotificationDTO.TypeNotification.REJET_ADMIN)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Notification de rejet envoyée au doctorant {} via Kafka", doctorantId);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de notification au doctorant {}: {}", doctorantId, e.getMessage());
        }
    }

    public void notifierValidationDefinitive(Long doctorantId, Long directeurId, Long inscriptionId) {
        log.info("Notification validation définitive - inscription {}", inscriptionId);
        
        try {
            UserDTO doctorant = userServiceClient.getUserById(doctorantId);
            UserDTO directeur = userServiceClient.getUserById(directeurId);
            
            // Notification au doctorant
            NotificationDTO notificationDoctorant = NotificationDTO.builder()
                    .destinataireEmail(doctorant.getEmail())
                    .destinataireNom(doctorant.getFirstName() + " " + doctorant.getLastName())
                    .sujet("Inscription validée")
                    .message(String.format(
                            "Bonjour %s,\n\nFélicitations ! Votre inscription (ID: %d) a été validée.\n\nVous pouvez télécharger votre attestation d'inscription.\n\nCordialement,\nService Doctorat",
                            doctorant.getFirstName(), inscriptionId
                    ))
                    .type(NotificationDTO.TypeNotification.VALIDATION_DEFINITIVE)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notificationDoctorant);
            
            // Notification au directeur
            NotificationDTO notificationDirecteur = NotificationDTO.builder()
                    .destinataireEmail(directeur.getEmail())
                    .destinataireNom(directeur.getFirstName() + " " + directeur.getLastName())
                    .sujet("Inscription validée")
                    .message(String.format(
                            "Bonjour %s,\n\nL'inscription de %s %s (ID: %d) a été validée par l'administration.\n\nCordialement,\nService Doctorat",
                            directeur.getFirstName(), doctorant.getFirstName(), doctorant.getLastName(), inscriptionId
                    ))
                    .type(NotificationDTO.TypeNotification.VALIDATION_DEFINITIVE)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notificationDirecteur);
            
            log.info("Notifications de validation définitive envoyées via Kafka");
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications de validation: {}", e.getMessage());
        }
    }

    public void genererAttestationInscription(Long inscriptionId) {
        log.info("Génération attestation pour inscription {}", inscriptionId);
        
        try {
            // Récupérer l'inscription avec toutes les informations
            Inscription inscription = inscriptionRepository.findById(inscriptionId)
                    .orElseThrow(() -> new RuntimeException("Inscription introuvable"));
            
            InfosDoctorant infosDoctorant = inscription.getInfosDoctorant();
            if (infosDoctorant == null) {
                log.error("Informations doctorant manquantes pour l'inscription {}", inscriptionId);
                throw new RuntimeException("Informations doctorant manquantes");
            }
            
            // Récupérer les informations du directeur avec la méthode dédiée
            // Requirements: 2.2 - Fetch director information for attestation
            UserDTO directeur = userServiceClient.getDirectorInfo(inscription.getDirecteurTheseId());
            
            // Générer l'attestation PDF
            String filePath = attestationPdfGenerator.generateAttestation(
                    inscription, 
                    infosDoctorant, 
                    directeur
            );
            
            log.info("Attestation générée avec succès pour l'inscription {}: {}", inscriptionId, filePath);
            
        } catch (Exception e) {
            log.error("Erreur lors de la génération de l'attestation pour l'inscription {}: {}", 
                    inscriptionId, e.getMessage(), e);
            // Ne pas bloquer le processus de validation si la génération échoue
            // L'attestation pourra être régénérée plus tard
        }
    }

    /**
     * Publish INSCRIPTION_SOUMISE event to Kafka
     * Requirements: 7.1
     * 
     * @param inscriptionId The inscription ID
     * @param doctorantId The student ID
     * @param directeurId The director ID
     */
    public void publierEvenementInscriptionSoumise(Long inscriptionId, Long doctorantId, Long directeurId) {
        log.info("Publication événement INSCRIPTION_SOUMISE pour inscription {}", inscriptionId);
        
        try {
            NotificationDTO notification = NotificationDTO.builder()
                    .sujet("Inscription soumise")
                    .message(String.format(
                            "L'inscription ID: %d a été soumise par le doctorant ID: %d pour validation par le directeur ID: %d",
                            inscriptionId, doctorantId, directeurId
                    ))
                    .type(NotificationDTO.TypeNotification.NOUVELLE_DEMANDE_DIRECTEUR)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Événement INSCRIPTION_SOUMISE publié pour inscription {}", inscriptionId);
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement INSCRIPTION_SOUMISE: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish INSCRIPTION_VALIDEE_DIRECTEUR event to Kafka
     * Requirements: 7.2
     * 
     * @param inscriptionId The inscription ID
     * @param directeurId The director ID
     */
    public void publierEvenementInscriptionValideeDirecteur(Long inscriptionId, Long directeurId) {
        log.info("Publication événement INSCRIPTION_VALIDEE_DIRECTEUR pour inscription {}", inscriptionId);
        
        try {
            NotificationDTO notification = NotificationDTO.builder()
                    .sujet("Inscription validée par le directeur")
                    .message(String.format(
                            "L'inscription ID: %d a été validée par le directeur ID: %d",
                            inscriptionId, directeurId
                    ))
                    .type(NotificationDTO.TypeNotification.VALIDATION_DIRECTEUR)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Événement INSCRIPTION_VALIDEE_DIRECTEUR publié pour inscription {}", inscriptionId);
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement INSCRIPTION_VALIDEE_DIRECTEUR: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish INSCRIPTION_REJETEE_DIRECTEUR event to Kafka
     * Requirements: 7.3
     * 
     * @param inscriptionId The inscription ID
     * @param directeurId The director ID
     * @param motifRejet The rejection reason
     */
    public void publierEvenementInscriptionRejeteeDirecteur(Long inscriptionId, Long directeurId, String motifRejet) {
        log.info("Publication événement INSCRIPTION_REJETEE_DIRECTEUR pour inscription {}", inscriptionId);
        
        try {
            NotificationDTO notification = NotificationDTO.builder()
                    .sujet("Inscription rejetée par le directeur")
                    .message(String.format(
                            "L'inscription ID: %d a été rejetée par le directeur ID: %d. Motif: %s",
                            inscriptionId, directeurId, motifRejet != null ? motifRejet : "Non spécifié"
                    ))
                    .type(NotificationDTO.TypeNotification.REJET_DIRECTEUR)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Événement INSCRIPTION_REJETEE_DIRECTEUR publié pour inscription {}", inscriptionId);
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement INSCRIPTION_REJETEE_DIRECTEUR: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish INSCRIPTION_VALIDEE_ADMIN event to Kafka
     * Requirements: 7.4
     * 
     * @param inscriptionId The inscription ID
     */
    public void publierEvenementInscriptionValideeAdmin(Long inscriptionId) {
        log.info("Publication événement INSCRIPTION_VALIDEE_ADMIN pour inscription {}", inscriptionId);
        
        try {
            NotificationDTO notification = NotificationDTO.builder()
                    .sujet("Inscription validée par l'administration")
                    .message(String.format(
                            "L'inscription ID: %d a été validée par l'administration",
                            inscriptionId
                    ))
                    .type(NotificationDTO.TypeNotification.VALIDATION_ADMIN)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Événement INSCRIPTION_VALIDEE_ADMIN publié pour inscription {}", inscriptionId);
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement INSCRIPTION_VALIDEE_ADMIN: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish INSCRIPTION_REJETEE_ADMIN event to Kafka
     * Requirements: 7.5
     * 
     * @param inscriptionId The inscription ID
     * @param motifRejet The rejection reason
     */
    public void publierEvenementInscriptionRejeteeAdmin(Long inscriptionId, String motifRejet) {
        log.info("Publication événement INSCRIPTION_REJETEE_ADMIN pour inscription {}", inscriptionId);
        
        try {
            NotificationDTO notification = NotificationDTO.builder()
                    .sujet("Inscription rejetée par l'administration")
                    .message(String.format(
                            "L'inscription ID: %d a été rejetée par l'administration. Motif: %s",
                            inscriptionId, motifRejet != null ? motifRejet : "Non spécifié"
                    ))
                    .type(NotificationDTO.TypeNotification.REJET_ADMIN)
                    .inscriptionId(inscriptionId)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Événement INSCRIPTION_REJETEE_ADMIN publié pour inscription {}", inscriptionId);
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement INSCRIPTION_REJETEE_ADMIN: {}", e.getMessage(), e);
        }
    }
}
