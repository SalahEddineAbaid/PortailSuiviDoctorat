package ma.emsi.inscriptionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.NotificationDTO;
import ma.emsi.inscriptionservice.DTOs.UserDTO;
import ma.emsi.inscriptionservice.client.UserServiceClient;
import ma.emsi.inscriptionservice.entities.AlerteDuree;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.TypeAlerte;
import ma.emsi.inscriptionservice.repositories.AlerteDureeRepository;
import ma.emsi.inscriptionservice.repositories.InscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlerteService {

    private final AlerteDureeRepository alerteDureeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final KafkaTemplate<String, NotificationDTO> kafkaTemplate;
    private final UserServiceClient userServiceClient;

    @Value("${alertes.duree.seuil-3-ans:2.5}")
    private double seuil3Ans;

    @Value("${alertes.duree.seuil-6-ans:5.5}")
    private double seuil6Ans;

    @Value("${alertes.duree.limite-max:6.0}")
    private double limiteMax;

    @Value("${kafka.topic.notifications:notifications}")
    private String notificationTopic;

    /**
     * Vérifie et génère les alertes de durée pour une inscription donnée
     * Vérifie les seuils de 2.5 ans, 5.5 ans et 6 ans
     * 
     * @param inscription L'inscription à vérifier
     */
    @Transactional
    public void verifierEtGenererAlertes(Inscription inscription) {
        log.info("Vérification des alertes pour l'inscription {}", inscription.getId());

        if (inscription.getDatePremiereInscription() == null) {
            log.warn("Date de première inscription manquante pour l'inscription {}", inscription.getId());
            return;
        }

        // Calculer la durée en années avec décimales
        double dureeEnAnnees = calculerDureeEnAnnees(inscription);
        log.debug("Durée calculée pour l'inscription {}: {} ans", inscription.getId(), dureeEnAnnees);

        // Vérifier le seuil de 2.5 ans (approche 3 ans)
        if (dureeEnAnnees >= seuil3Ans && !alerteExiste(inscription, TypeAlerte.APPROCHE_3_ANS)) {
            log.info("Création alerte APPROCHE_3_ANS pour l'inscription {}", inscription.getId());
            creerAlerte(inscription, TypeAlerte.APPROCHE_3_ANS);
        }

        // Vérifier le seuil de 5.5 ans (approche 6 ans)
        if (dureeEnAnnees >= seuil6Ans && !alerteExiste(inscription, TypeAlerte.APPROCHE_6_ANS)) {
            log.info("Création alerte APPROCHE_6_ANS pour l'inscription {}", inscription.getId());
            creerAlerte(inscription, TypeAlerte.APPROCHE_6_ANS);
        }

        // Vérifier le seuil de 6 ans (dépassement)
        if (dureeEnAnnees >= limiteMax && !alerteExiste(inscription, TypeAlerte.DEPASSE_6_ANS)) {
            log.info("Création alerte DEPASSE_6_ANS pour l'inscription {}", inscription.getId());
            creerAlerte(inscription, TypeAlerte.DEPASSE_6_ANS);
            
            // Bloquer la réinscription
            inscription.setBloqueReInscription(true);
            inscriptionRepository.save(inscription);
            log.info("Réinscription bloquée pour l'inscription {}", inscription.getId());
        }
    }

    /**
     * Calcule la durée en années avec décimales depuis la première inscription
     * 
     * @param inscription L'inscription
     * @return La durée en années (avec décimales)
     */
    private double calculerDureeEnAnnees(Inscription inscription) {
        if (inscription.getDatePremiereInscription() == null) {
            return 0.0;
        }
        
        long joursEcoules = ChronoUnit.DAYS.between(
            inscription.getDatePremiereInscription(), 
            LocalDateTime.now()
        );
        
        // Convertir en années (365.25 jours par an pour tenir compte des années bissextiles)
        return joursEcoules / 365.25;
    }

    /**
     * Vérifie si une alerte du type spécifié existe déjà pour l'inscription
     * 
     * @param inscription L'inscription
     * @param type Le type d'alerte
     * @return true si l'alerte existe déjà, false sinon
     */
    public boolean alerteExiste(Inscription inscription, TypeAlerte type) {
        Long count = alerteDureeRepository.countByInscriptionIdAndType(inscription.getId(), type);
        return count != null && count > 0;
    }

    /**
     * Crée une nouvelle alerte et publie une notification Kafka
     * 
     * @param inscription L'inscription concernée
     * @param type Le type d'alerte
     */
    @Transactional
    public void creerAlerte(Inscription inscription, TypeAlerte type) {
        log.info("Création d'une alerte {} pour l'inscription {}", type, inscription.getId());

        // Créer l'alerte
        AlerteDuree alerte = AlerteDuree.builder()
                .inscription(inscription)
                .type(type)
                .dateAlerte(LocalDateTime.now())
                .traite(false)
                .action(genererMessageAction(type))
                .build();

        alerteDureeRepository.save(alerte);
        log.info("Alerte {} créée avec succès (ID: {})", type, alerte.getId());

        // Publier la notification Kafka
        publierNotificationAlerte(inscription, type);
    }

    /**
     * Génère le message d'action approprié selon le type d'alerte
     * 
     * @param type Le type d'alerte
     * @return Le message d'action
     */
    private String genererMessageAction(TypeAlerte type) {
        return switch (type) {
            case APPROCHE_3_ANS -> 
                "Vous approchez de la limite de 3 ans. Pensez à demander une dérogation si nécessaire.";
            case APPROCHE_6_ANS -> 
                "Vous approchez de la limite maximale de 6 ans. Une action urgente est requise.";
            case DEPASSE_6_ANS -> 
                "La limite maximale de 6 ans est dépassée. Réinscription bloquée.";
        };
    }

    /**
     * Publie une notification Kafka pour l'alerte créée
     * Requirement 7.9: Publish ALERTE_DUREE event
     * 
     * @param inscription L'inscription concernée
     * @param type Le type d'alerte
     */
    private void publierNotificationAlerte(Inscription inscription, TypeAlerte type) {
        try {
            // Récupérer les informations du doctorant
            UserDTO doctorant = userServiceClient.getUserById(inscription.getDoctorantId());
            
            String sujet = genererSujetNotification(type);
            String message = genererMessageNotification(type, doctorant.getFirstName(), inscription);

            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail(doctorant.getEmail())
                    .destinataireNom(doctorant.getFirstName() + " " + doctorant.getLastName())
                    .sujet(sujet)
                    .message(message)
                    .type(NotificationDTO.TypeNotification.RAPPEL_DOCUMENTS) // Utiliser un type existant
                    .inscriptionId(inscription.getId())
                    .dateEnvoi(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(notificationTopic, notification);
            log.info("Événement ALERTE_DUREE publié pour l'alerte {} de l'inscription {}", 
                    type, inscription.getId());
            
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement ALERTE_DUREE pour l'alerte {} " +
                    "de l'inscription {}: {}", type, inscription.getId(), e.getMessage());
            // Ne pas bloquer le processus si la notification échoue
        }
    }

    /**
     * Génère le sujet de la notification selon le type d'alerte
     * 
     * @param type Le type d'alerte
     * @return Le sujet de la notification
     */
    private String genererSujetNotification(TypeAlerte type) {
        return switch (type) {
            case APPROCHE_3_ANS -> "Alerte : Approche de la limite de 3 ans";
            case APPROCHE_6_ANS -> "Alerte urgente : Approche de la limite de 6 ans";
            case DEPASSE_6_ANS -> "Alerte critique : Limite de 6 ans dépassée";
        };
    }

    /**
     * Génère le message de la notification selon le type d'alerte
     * 
     * @param type Le type d'alerte
     * @param prenom Le prénom du doctorant
     * @param inscription L'inscription concernée
     * @return Le message de la notification
     */
    private String genererMessageNotification(TypeAlerte type, String prenom, Inscription inscription) {
        double duree = calculerDureeEnAnnees(inscription);
        
        return switch (type) {
            case APPROCHE_3_ANS -> String.format(
                    "Bonjour %s,\n\n" +
                    "Vous êtes inscrit(e) en doctorat depuis %.1f ans.\n\n" +
                    "Vous approchez de la limite standard de 3 ans. Si vous pensez avoir besoin " +
                    "de plus de temps pour finaliser votre thèse, nous vous recommandons de " +
                    "préparer une demande de dérogation.\n\n" +
                    "Cordialement,\n" +
                    "Service Doctorat",
                    prenom, duree
            );
            case APPROCHE_6_ANS -> String.format(
                    "Bonjour %s,\n\n" +
                    "Vous êtes inscrit(e) en doctorat depuis %.1f ans.\n\n" +
                    "ATTENTION : Vous approchez de la limite maximale de 6 ans. Au-delà de cette " +
                    "limite, la réinscription sera automatiquement bloquée.\n\n" +
                    "Veuillez prendre les dispositions nécessaires pour finaliser votre thèse " +
                    "dans les meilleurs délais.\n\n" +
                    "Cordialement,\n" +
                    "Service Doctorat",
                    prenom, duree
            );
            case DEPASSE_6_ANS -> String.format(
                    "Bonjour %s,\n\n" +
                    "Vous êtes inscrit(e) en doctorat depuis %.1f ans.\n\n" +
                    "La limite maximale de 6 ans a été dépassée. Votre réinscription est " +
                    "désormais bloquée conformément au règlement des études doctorales.\n\n" +
                    "Veuillez contacter le service doctorat pour examiner votre situation.\n\n" +
                    "Cordialement,\n" +
                    "Service Doctorat",
                    prenom, duree
            );
        };
    }

    /**
     * Récupère toutes les alertes actives pour un doctorant
     * 
     * @param doctorantId L'identifiant du doctorant
     * @return La liste des alertes actives
     */
    public List<AlerteDuree> getAlertesActives(Long doctorantId) {
        log.debug("Récupération des alertes actives pour le doctorant {}", doctorantId);
        return alerteDureeRepository.findAlertesActivesByDoctorant(doctorantId);
    }

    /**
     * Vérifie et génère les alertes pour toutes les inscriptions actives (batch processing)
     * Cette méthode est destinée à être appelée par un service batch ou un endpoint administratif
     * 
     * Requirements: 4.1, 4.2, 4.3
     * 
     * @param inscriptions Liste des inscriptions à vérifier
     * @return Résumé de la vérification avec statistiques
     */
    @Transactional
    public ma.emsi.inscriptionservice.DTOs.AlerteVerificationSummary verifierAlertesEnBatch(
            List<Inscription> inscriptions) {
        
        log.info("Démarrage de la vérification des alertes en batch pour {} inscriptions", 
                inscriptions.size());
        
        long startTime = System.currentTimeMillis();
        
        int totalAlertesGenerees = 0;
        int inscriptionsBloqueees = 0;
        java.util.Map<String, Integer> alertesParType = new java.util.HashMap<>();
        alertesParType.put("APPROCHE_3_ANS", 0);
        alertesParType.put("APPROCHE_6_ANS", 0);
        alertesParType.put("DEPASSE_6_ANS", 0);
        
        for (Inscription inscription : inscriptions) {
            try {
                // Compter les alertes avant vérification
                int alertesAvant = alerteDureeRepository
                        .findByInscriptionId(inscription.getId())
                        .size();
                
                // Vérifier et générer les alertes
                verifierEtGenererAlertes(inscription);
                
                // Compter les alertes après vérification
                List<AlerteDuree> alertesApres = alerteDureeRepository
                        .findByInscriptionId(inscription.getId());
                
                int nouvellesAlertes = alertesApres.size() - alertesAvant;
                totalAlertesGenerees += nouvellesAlertes;
                
                // Compter par type
                for (AlerteDuree alerte : alertesApres) {
                    String typeStr = alerte.getType().name();
                    alertesParType.put(typeStr, alertesParType.getOrDefault(typeStr, 0) + 1);
                }
                
                // Vérifier si l'inscription a été bloquée
                Inscription inscriptionMiseAJour = inscriptionRepository
                        .findById(inscription.getId())
                        .orElse(inscription);
                
                if (inscriptionMiseAJour.isBloqueReInscription() && 
                    !inscription.isBloqueReInscription()) {
                    inscriptionsBloqueees++;
                }
                
            } catch (Exception e) {
                log.error("Erreur lors de la vérification des alertes pour l'inscription {}: {}", 
                        inscription.getId(), e.getMessage());
                // Continuer avec les autres inscriptions
            }
        }
        
        long endTime = System.currentTimeMillis();
        long dureeTraitement = endTime - startTime;
        
        String message = String.format(
                "Vérification terminée: %d inscriptions vérifiées, %d alertes générées, %d inscriptions bloquées",
                inscriptions.size(), totalAlertesGenerees, inscriptionsBloqueees
        );
        
        log.info(message);
        
        return ma.emsi.inscriptionservice.DTOs.AlerteVerificationSummary.builder()
                .totalInscriptionsVerifiees(inscriptions.size())
                .totalAlertesGenerees(totalAlertesGenerees)
                .alertesParType(alertesParType)
                .inscriptionsBloqueees(inscriptionsBloqueees)
                .dateVerification(LocalDateTime.now())
                .dureeTraitementMs(dureeTraitement)
                .message(message)
                .build();
    }
}
