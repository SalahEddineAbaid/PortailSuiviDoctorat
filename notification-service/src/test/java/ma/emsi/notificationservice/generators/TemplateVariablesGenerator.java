package ma.emsi.notificationservice.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom generator for template variable maps for property-based testing.
 * Generates random maps with various combinations of template variables
 * used in email templates.
 */
public class TemplateVariablesGenerator extends Generator<Map<String, Object>> {
    
    private static final String[] VARIABLE_KEYS = {
        // Person-related variables
        "nomDoctorant", "prenomDoctorant", "emailDoctorant",
        "nomDirecteur", "prenomDirecteur", "emailDirecteur",
        "nomAdmin", "prenomAdmin", "emailAdmin",
        "nomJury", "prenomJury", "emailJury",
        
        // Document-related variables
        "titreSujet", "numeroInscription", "numeroDossier",
        "typeDocument", "nomDocument",
        
        // Date-related variables
        "dateInscription", "dateSoutenance", "dateEcheance",
        "dateValidation", "dateRejet", "dateRappel",
        
        // Location-related variables
        "lieuSoutenance", "salle", "batiment",
        
        // Status-related variables
        "statut", "commentaire", "motifRejet", "motifDerogation",
        
        // Academic-related variables
        "anneeUniversitaire", "specialite", "laboratoire",
        "etablissement", "cedDoc",
        
        // Link-related variables
        "lienPortail", "lienDocument", "lienConfirmation",
        
        // Other variables
        "heureDebut", "heureFin", "duree", "message"
    };
    
    private static final String[] FIRST_NAMES = {
        "Ahmed", "Fatima", "Mohammed", "Aisha", "Youssef", "Sara", 
        "Omar", "Leila", "Hassan", "Nadia", "Karim", "Samira"
    };
    
    private static final String[] LAST_NAMES = {
        "Alami", "Benali", "Idrissi", "El Fassi", "Tazi", "Benjelloun",
        "El Amrani", "Kadiri", "Berrada", "Chraibi", "Lahlou", "Mansouri"
    };
    
    private static final String[] THESIS_TITLES = {
        "Intelligence Artificielle et Apprentissage Automatique",
        "Systèmes Distribués et Cloud Computing",
        "Sécurité des Réseaux et Cryptographie",
        "Big Data et Analyse de Données",
        "Internet des Objets et Systèmes Embarqués",
        "Blockchain et Technologies Décentralisées",
        "Vision par Ordinateur et Traitement d'Images",
        "Traitement Automatique du Langage Naturel"
    };
    
    private static final String[] LOCATIONS = {
        "Amphithéâtre A", "Amphithéâtre B", "Salle de conférence",
        "Salle des thèses", "Auditorium principal", "Salle 101"
    };
    
    private static final String[] COMMENTS = {
        "Dossier complet et conforme aux exigences",
        "Documents manquants - CV et diplômes",
        "Sujet de recherche bien défini",
        "Nécessite des clarifications sur le plan de recherche",
        "Excellent parcours académique",
        "Validation en attente de documents complémentaires"
    };
    
    private static final String[] SPECIALTIES = {
        "Informatique", "Mathématiques Appliquées", "Physique",
        "Génie Électrique", "Génie Mécanique", "Sciences de Gestion"
    };
    
    private static final String[] LABORATORIES = {
        "LISI", "LIMIARF", "LRIT", "LASTID", "LERMA", "LMCSA"
    };
    
    @SuppressWarnings("unchecked")
    public TemplateVariablesGenerator() {
        super((Class<Map<String, Object>>) (Class<?>) Map.class);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> generate(SourceOfRandomness random, GenerationStatus status) {
        Map<String, Object> variables = new HashMap<>();
        
        // 10% chance of generating an empty map (edge case)
        if (random.nextInt(10) == 0) {
            return variables;
        }
        
        // Randomly decide how many variables to include (1-15)
        int numVariables = random.nextInt(1, 16);
        
        // Shuffle and select random keys
        for (int i = 0; i < numVariables && i < VARIABLE_KEYS.length; i++) {
            String key = VARIABLE_KEYS[random.nextInt(VARIABLE_KEYS.length)];
            
            // Skip if key already exists
            if (variables.containsKey(key)) {
                continue;
            }
            
            Object value = generateValueForKey(key, random);
            variables.put(key, value);
        }
        
        // Ensure at least one variable is present (unless we're testing empty map)
        if (variables.isEmpty() && random.nextInt(10) != 0) {
            variables.put("message", "Test message");
        }
        
        return variables;
    }
    
    /**
     * Generates an appropriate value based on the variable key.
     */
    private Object generateValueForKey(String key, SourceOfRandomness random) {
        // Name fields
        if (key.contains("nom") && !key.contains("Document")) {
            return LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        }
        
        if (key.contains("prenom")) {
            return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        }
        
        // Email fields
        if (key.contains("email")) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)].toLowerCase();
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)].toLowerCase().replace(" ", "");
            return firstName + "." + lastName + "@emsi.ma";
        }
        
        // Date fields
        if (key.contains("date")) {
            LocalDate date = LocalDate.now().plusDays(random.nextInt(-365, 365));
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        
        // Time fields
        if (key.contains("heure")) {
            int hour = random.nextInt(8, 19);
            int minute = random.nextInt(0, 60);
            return String.format("%02d:%02d", hour, minute);
        }
        
        // Title fields
        if (key.contains("titre")) {
            return THESIS_TITLES[random.nextInt(THESIS_TITLES.length)];
        }
        
        // Location fields
        if (key.contains("lieu") || key.contains("salle")) {
            return LOCATIONS[random.nextInt(LOCATIONS.length)];
        }
        
        // Comment/motif fields
        if (key.contains("commentaire") || key.contains("motif")) {
            return COMMENTS[random.nextInt(COMMENTS.length)];
        }
        
        // Specialty field
        if (key.contains("specialite")) {
            return SPECIALTIES[random.nextInt(SPECIALTIES.length)];
        }
        
        // Laboratory field
        if (key.contains("laboratoire")) {
            return LABORATORIES[random.nextInt(LABORATORIES.length)];
        }
        
        // Number fields
        if (key.contains("numero")) {
            return "INS-2024-" + String.format("%04d", random.nextInt(1, 10000));
        }
        
        // Year field
        if (key.contains("annee")) {
            int year = LocalDate.now().getYear();
            return year + "-" + (year + 1);
        }
        
        // Link fields
        if (key.contains("lien")) {
            return "https://portail-doctorat.ma/" + key.replace("lien", "").toLowerCase();
        }
        
        // Duration field
        if (key.contains("duree")) {
            return random.nextInt(1, 5) + " heures";
        }
        
        // Default: return a generic string
        return "Valeur pour " + key;
    }
}
