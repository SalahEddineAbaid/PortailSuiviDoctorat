package ma.emsi.inscriptionservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.CampagneRequest;
import ma.emsi.inscriptionservice.DTOs.CampagneResponse;
import ma.emsi.inscriptionservice.DTOs.NotificationDTO;
import ma.emsi.inscriptionservice.DTOs.StatistiquesCampagne;
import ma.emsi.inscriptionservice.entities.Campagne;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.repositories.CampagneRepository;
import ma.emsi.inscriptionservice.repositories.InscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampagneService {

    private final CampagneRepository campagneRepository;
    private final InscriptionRepository inscriptionRepository;
    private final KafkaTemplate<String, NotificationDTO> kafkaTemplate;

    @Value("${kafka.topic.notifications:notifications}")
    private String notificationTopic;

    @Transactional
    public CampagneResponse creerCampagne(CampagneRequest request) {
        log.info("Création d'une nouvelle campagne: {}", request.getLibelle());

        // Vérifier qu'il n'y a pas déjà une campagne pour cette année
        campagneRepository.findByTypeAndAnneeUniversitaire(
                request.getType(),
                request.getAnneeUniversitaire()
        ).ifPresent(c -> {
            throw new RuntimeException("Une campagne existe déjà pour cette année universitaire");
        });

        Campagne campagne = Campagne.builder()
                .libelle(request.getLibelle())
                .type(request.getType())
                .dateDebut(request.getDateDebut())
                .dateFin(request.getDateFin())
                .anneeUniversitaire(request.getAnneeUniversitaire())
                .active(true)
                .build();

        campagne = campagneRepository.save(campagne);

        log.info("Campagne créée: ID {}", campagne.getId());

        return mapToResponse(campagne);
    }

    public CampagneResponse getCampagne(Long id) {
        Campagne campagne = campagneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));
        return mapToResponse(campagne);
    }

    public List<CampagneResponse> getAllCampagnes() {
        return campagneRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CampagneResponse> getCampagnesActives() {
        return campagneRepository.findAll()
                .stream()
                .filter(Campagne::isOuverte)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CampagneResponse fermerCampagne(Long id) {
        log.info("Fermeture de la campagne {}", id);

        Campagne campagne = campagneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));

        campagne.fermer();
        campagne = campagneRepository.save(campagne);

        return mapToResponse(campagne);
    }

    @Transactional
    public CampagneResponse modifierCampagne(Long id, CampagneRequest request) {
        log.info("Modification de la campagne {}", id);

        Campagne campagne = campagneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));

        campagne.setLibelle(request.getLibelle());
        campagne.setDateDebut(request.getDateDebut());
        campagne.setDateFin(request.getDateFin());

        campagne = campagneRepository.save(campagne);

        return mapToResponse(campagne);
    }

    /**
     * Get statistics for a campaign
     * Calculates total inscriptions, breakdown by status, validation rate, and average validation time
     */
    public StatistiquesCampagne getStatistiques(Long campagneId) {
        log.info("Calcul des statistiques pour la campagne {}", campagneId);

        Campagne campagne = campagneRepository.findById(campagneId)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));

        // Get all inscriptions for this campaign
        List<Inscription> inscriptions = inscriptionRepository.findByCampagneId(campagneId);
        int nombreInscriptions = inscriptions.size();

        // Calculate breakdown by status
        Map<StatutInscription, Integer> parStatut = new HashMap<>();
        for (StatutInscription statut : StatutInscription.values()) {
            long count = inscriptions.stream()
                    .filter(i -> i.getStatut() == statut)
                    .count();
            parStatut.put(statut, (int) count);
        }

        // Calculate validation rate
        long nombreValides = inscriptions.stream()
                .filter(i -> i.getStatut() == StatutInscription.VALIDE)
                .count();
        double tauxValidation = nombreInscriptions > 0 
                ? (nombreValides * 100.0) / nombreInscriptions 
                : 0.0;

        // Calculate average validation time in days
        double tempsMoyenValidation = inscriptions.stream()
                .filter(i -> i.getStatut() == StatutInscription.VALIDE && i.getDateValidation() != null)
                .mapToLong(i -> ChronoUnit.DAYS.between(i.getDateCreation(), i.getDateValidation()))
                .average()
                .orElse(0.0);

        log.info("Statistiques calculées pour la campagne {}: {} inscriptions, taux de validation: {}%", 
                campagneId, nombreInscriptions, String.format("%.2f", tauxValidation));

        return StatistiquesCampagne.builder()
                .campagneId(campagne.getId())
                .libelle(campagne.getLibelle())
                .type(campagne.getType())
                .nombreInscriptions(nombreInscriptions)
                .parStatut(parStatut)
                .tauxValidation(tauxValidation)
                .tempsMoyenValidation(tempsMoyenValidation)
                .build();
    }

    /**
     * Clone an existing campaign with new dates
     * Copies campaign type and libelle, increments year in libelle, sets new dates, and sets active flag to false
     * 
     * @param campagneId ID of the campaign to clone
     * @param dateDebut Start date for the new campaign
     * @param dateFin End date for the new campaign
     * @return The newly created campaign
     */
    @Transactional
    public CampagneResponse clonerCampagne(Long campagneId, LocalDate dateDebut, LocalDate dateFin) {
        log.info("Clonage de la campagne {} avec nouvelles dates: {} - {}", campagneId, dateDebut, dateFin);

        // Récupérer la campagne source
        Campagne campagneSource = campagneRepository.findById(campagneId)
                .orElseThrow(() -> new RuntimeException("Campagne introuvable"));

        // Extraire et incrémenter l'année dans le libellé
        String nouveauLibelle = incrementerAnneeLibelle(campagneSource.getLibelle());

        // Calculer la nouvelle année universitaire basée sur la date de début
        Integer nouvelleAnneeUniversitaire = dateDebut.getYear();

        // Créer la nouvelle campagne
        Campagne nouvelleCampagne = Campagne.builder()
                .libelle(nouveauLibelle)
                .type(campagneSource.getType())
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .anneeUniversitaire(nouvelleAnneeUniversitaire)
                .active(false) // Initialement inactive
                .build();

        nouvelleCampagne = campagneRepository.save(nouvelleCampagne);

        log.info("Campagne clonée avec succès: ID {} -> ID {}", campagneId, nouvelleCampagne.getId());

        return mapToResponse(nouvelleCampagne);
    }

    /**
     * Increments the year in a campaign label
     * Searches for a 4-digit year pattern and increments it by 1
     * If no year is found, appends the next year
     * 
     * @param libelle The original label
     * @return The label with incremented year
     */
    private String incrementerAnneeLibelle(String libelle) {
        // Rechercher un pattern d'année (4 chiffres)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{4})");
        java.util.regex.Matcher matcher = pattern.matcher(libelle);
        
        if (matcher.find()) {
            // Extraire l'année et l'incrémenter
            int annee = Integer.parseInt(matcher.group(1));
            int nouvelleAnnee = annee + 1;
            
            // Remplacer l'année dans le libellé
            return matcher.replaceFirst(String.valueOf(nouvelleAnnee));
        } else {
            // Si aucune année n'est trouvée, ajouter l'année suivante
            int anneeSuivante = LocalDate.now().getYear() + 1;
            return libelle + " " + anneeSuivante;
        }
    }

    /**
     * Scheduled task that runs daily at 8:00 AM to verify campaigns
     * for opening or closing based on current date
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void verifierCampagnes() {
        log.info("Vérification des campagnes - Tâche planifiée démarrée");
        
        LocalDate today = LocalDate.now();
        
        // Vérifier les campagnes qui doivent s'ouvrir aujourd'hui
        List<Campagne> campagnesAOuvrir = campagneRepository.findByDateDebut(today);
        for (Campagne campagne : campagnesAOuvrir) {
            if (campagne.getActive() && !campagne.isOuverte()) {
                log.info("Ouverture de la campagne: {} (ID: {})", campagne.getLibelle(), campagne.getId());
                ouvrirCampagne(campagne);
            }
        }
        
        // Vérifier les campagnes qui doivent se fermer aujourd'hui
        List<Campagne> campagnesAFermer = campagneRepository.findByDateFin(today);
        for (Campagne campagne : campagnesAFermer) {
            if (campagne.getActive() && campagne.isOuverte()) {
                log.info("Fermeture automatique de la campagne: {} (ID: {})", campagne.getLibelle(), campagne.getId());
                fermerCampagneAutomatique(campagne);
            }
        }
        
        log.info("Vérification des campagnes terminée - {} campagnes ouvertes, {} campagnes fermées", 
                campagnesAOuvrir.size(), campagnesAFermer.size());
    }

    /**
     * Opens a campaign and sends opening notifications to eligible students
     */
    private void ouvrirCampagne(Campagne campagne) {
        try {
            // Publier l'événement Kafka pour l'ouverture de la campagne
            publierEvenementCampagneOuverte(campagne);
            
            // Envoyer des notifications aux étudiants éligibles
            envoyerNotificationsOuverture(campagne);
            
            log.info("Campagne {} ouverte avec succès", campagne.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'ouverture de la campagne {}: {}", campagne.getId(), e.getMessage(), e);
        }
    }

    /**
     * Closes a campaign automatically and sends closing notifications
     */
    private void fermerCampagneAutomatique(Campagne campagne) {
        try {
            // Mettre à jour le flag active
            campagne.setActive(false);
            campagneRepository.save(campagne);
            
            // Publier l'événement Kafka pour la fermeture de la campagne
            publierEvenementCampagneFermee(campagne);
            
            // Envoyer des notifications de fermeture
            envoyerNotificationsFermeture(campagne);
            
            log.info("Campagne {} fermée automatiquement avec succès", campagne.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la fermeture automatique de la campagne {}: {}", campagne.getId(), e.getMessage(), e);
        }
    }

    /**
     * Publishes a Kafka event when a campaign opens
     */
    private void publierEvenementCampagneOuverte(Campagne campagne) {
        try {
            NotificationDTO notification = NotificationDTO.builder()
                    .sujet("Ouverture de campagne: " + campagne.getLibelle())
                    .message(String.format(
                            "La campagne %s est maintenant ouverte du %s au %s.",
                            campagne.getLibelle(),
                            campagne.getDateDebut(),
                            campagne.getDateFin()
                    ))
                    .type(NotificationDTO.TypeNotification.CAMPAGNE_OUVERTE)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Événement CAMPAGNE_OUVERTE publié pour la campagne {}", campagne.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement CAMPAGNE_OUVERTE: {}", e.getMessage(), e);
        }
    }

    /**
     * Publishes a Kafka event when a campaign closes
     */
    private void publierEvenementCampagneFermee(Campagne campagne) {
        try {
            NotificationDTO notification = NotificationDTO.builder()
                    .sujet("Fermeture de campagne: " + campagne.getLibelle())
                    .message(String.format(
                            "La campagne %s est maintenant fermée. Date de fermeture: %s.",
                            campagne.getLibelle(),
                            campagne.getDateFin()
                    ))
                    .type(NotificationDTO.TypeNotification.CAMPAGNE_FERMEE)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Événement CAMPAGNE_FERMEE publié pour la campagne {}", campagne.getId());
        } catch (Exception e) {
            log.error("Erreur lors de la publication de l'événement CAMPAGNE_FERMEE: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends opening notifications to all eligible students
     */
    private void envoyerNotificationsOuverture(Campagne campagne) {
        try {
            // Note: Dans une implémentation complète, nous récupérerions la liste des étudiants éligibles
            // depuis le user-service via Feign Client et enverrions des notifications individuelles.
            // Pour l'instant, nous publions une notification générale via Kafka.
            
            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail("doctorants@emsi.ma") // Email générique pour tous les doctorants
                    .destinataireNom("Doctorants")
                    .sujet("Nouvelle campagne d'inscription ouverte")
                    .message(String.format(
                            "Bonjour,\n\n" +
                            "La campagne %s est maintenant ouverte.\n\n" +
                            "Type: %s\n" +
                            "Période: du %s au %s\n\n" +
                            "Veuillez soumettre votre dossier d'inscription avant la date limite.\n\n" +
                            "Cordialement,\n" +
                            "Service Doctorat",
                            campagne.getLibelle(),
                            campagne.getType(),
                            campagne.getDateDebut(),
                            campagne.getDateFin()
                    ))
                    .type(NotificationDTO.TypeNotification.CAMPAGNE_OUVERTE)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Notifications d'ouverture envoyées pour la campagne {}", campagne.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications d'ouverture: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends closing notifications
     */
    private void envoyerNotificationsFermeture(Campagne campagne) {
        try {
            NotificationDTO notification = NotificationDTO.builder()
                    .destinataireEmail("doctorants@emsi.ma") // Email générique pour tous les doctorants
                    .destinataireNom("Doctorants")
                    .sujet("Fermeture de campagne d'inscription")
                    .message(String.format(
                            "Bonjour,\n\n" +
                            "La campagne %s est maintenant fermée.\n\n" +
                            "Date de fermeture: %s\n\n" +
                            "Aucune nouvelle inscription ne sera acceptée pour cette campagne.\n\n" +
                            "Cordialement,\n" +
                            "Service Doctorat",
                            campagne.getLibelle(),
                            campagne.getDateFin()
                    ))
                    .type(NotificationDTO.TypeNotification.CAMPAGNE_FERMEE)
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(notificationTopic, notification);
            log.info("Notifications de fermeture envoyées pour la campagne {}", campagne.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des notifications de fermeture: {}", e.getMessage(), e);
        }
    }

    private CampagneResponse mapToResponse(Campagne campagne) {
        return CampagneResponse.builder()
                .id(campagne.getId())
                .libelle(campagne.getLibelle())
                .type(campagne.getType())
                .dateDebut(campagne.getDateDebut())
                .dateFin(campagne.getDateFin())
                .active(campagne.getActive())
                .anneeUniversitaire(campagne.getAnneeUniversitaire())
                .ouverte(campagne.isOuverte())
                .build();
    }
}

