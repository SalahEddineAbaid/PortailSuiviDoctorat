package ma.emsi.inscriptionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.client.UserServiceClient;
import ma.emsi.inscriptionservice.DTOs.NotificationDTO;
import ma.emsi.inscriptionservice.DTOs.UserDTO;
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
        // TODO: Implémenter la génération PDF avec iText
    }
}
