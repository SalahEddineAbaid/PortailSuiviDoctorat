package ma.emsi.notificationservice.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.TypeNotification;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom generator for NotificationDTO objects for property-based testing.
 * Generates random valid NotificationDTO instances with various combinations of fields.
 */
public class NotificationDTOGenerator extends Generator<NotificationDTO> {
    
    private static final String[] VALID_EMAIL_DOMAINS = {
        "@emsi.ma", "@portail-doctorat.ma", "@gmail.com", "@yahoo.fr", "@outlook.com"
    };
    
    private static final String[] FIRST_NAMES = {
        "ahmed", "fatima", "mohammed", "aisha", "youssef", "sara", "omar", "leila"
    };
    
    private static final String[] LAST_NAMES = {
        "alami", "benali", "idrissi", "el-fassi", "tazi", "benjelloun", "el-amrani", "kadiri"
    };
    
    private static final String[] SUBJECTS = {
        "Notification d'inscription",
        "Validation de dossier",
        "Demande de soutenance",
        "Invitation jury",
        "Rappel important",
        "Confirmation",
        "Alerte système"
    };
    
    private static final String[] MESSAGE_TEMPLATES = {
        "Votre dossier a été traité avec succès.",
        "Une action est requise de votre part.",
        "Veuillez consulter votre espace personnel.",
        "Votre demande est en cours de traitement.",
        "Merci de votre attention."
    };
    
    public NotificationDTOGenerator() {
        super(NotificationDTO.class);
    }
    
    @Override
    public NotificationDTO generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate valid email
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String domain = VALID_EMAIL_DOMAINS[random.nextInt(VALID_EMAIL_DOMAINS.length)];
        String email = firstName + "." + lastName + domain;
        
        // Generate random type
        TypeNotification[] types = TypeNotification.values();
        TypeNotification type = types[random.nextInt(types.length)];
        
        // Generate random priority
        PrioriteNotification[] priorities = PrioriteNotification.values();
        PrioriteNotification priorite = priorities[random.nextInt(priorities.length)];
        
        // Generate random subject
        String sujet = SUBJECTS[random.nextInt(SUBJECTS.length)];
        
        // Generate random message text (sometimes null to test edge case)
        String messageTexte = random.nextBoolean() 
            ? MESSAGE_TEMPLATES[random.nextInt(MESSAGE_TEMPLATES.length)]
            : null;
        
        // Generate random data map (sometimes empty to test edge case)
        Map<String, Object> donnees = random.nextInt(10) < 8 
            ? generateRandomDataMap(random) 
            : new HashMap<>();
        
        return NotificationDTO.builder()
            .type(type)
            .destinataire(email)
            .sujet(sujet)
            .messageTexte(messageTexte)
            .priorite(priorite)
            .donnees(donnees)
            .build();
    }
    
    /**
     * Generates a random map of template variables.
     * Includes common variables used in email templates.
     */
    private Map<String, Object> generateRandomDataMap(SourceOfRandomness random) {
        Map<String, Object> data = new HashMap<>();
        
        // Randomly decide how many variables to include (0-10)
        int numVariables = random.nextInt(11);
        
        String[] possibleKeys = {
            "nomDoctorant", "prenomDoctorant", "nomDirecteur", "prenomDirecteur",
            "titreSujet", "dateInscription", "dateSoutenance", "lieuSoutenance",
            "commentaire", "motifRejet", "nomJury", "dateEcheance", "lienPortail",
            "numeroInscription", "anneeUniversitaire", "specialite", "laboratoire"
        };
        
        String[] possibleValues = {
            "Ahmed Alami", "Fatima Benali", "Mohammed Idrissi", "Sara Tazi",
            "Intelligence Artificielle et Big Data", "Systèmes Distribués",
            "2024-01-15", "2024-06-20", "Amphithéâtre A", "Salle de conférence",
            "Dossier complet et conforme", "Documents manquants",
            "https://portail-doctorat.ma", "INS-2024-001", "2023-2024",
            "Informatique", "LISI"
        };
        
        for (int i = 0; i < numVariables && i < possibleKeys.length; i++) {
            String key = possibleKeys[random.nextInt(possibleKeys.length)];
            Object value = possibleValues[random.nextInt(possibleValues.length)];
            data.put(key, value);
        }
        
        return data;
    }
}
