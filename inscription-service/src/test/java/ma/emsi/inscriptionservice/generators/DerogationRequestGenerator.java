package ma.emsi.inscriptionservice.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import ma.emsi.inscriptionservice.entities.DerogationRequest;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.StatutDerogation;

import java.time.LocalDateTime;

/**
 * Custom generator for DerogationRequest entities for property-based testing.
 * Generates random derogation requests with valid and edge-case data.
 */
public class DerogationRequestGenerator extends Generator<DerogationRequest> {

    private static final String[] MOTIF_TEMPLATES = {
        "Retard dû à des problèmes de santé",
        "Difficultés techniques dans la recherche",
        "Changement d'orientation de la thèse",
        "Problèmes familiaux",
        "Manque de financement",
        "Complexité du sujet de recherche",
        "Retard dans la collecte de données",
        "Problèmes avec l'équipement de laboratoire",
        "Nécessité de publications supplémentaires",
        "Collaboration internationale retardée"
    };

    public DerogationRequestGenerator() {
        super(DerogationRequest.class);
    }

    @Override
    public DerogationRequest generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate random motif
        String motifTemplate = random.choose(MOTIF_TEMPLATES);
        String motif = motifTemplate + " - Détails: " + random.nextInt(1, 1000);
        
        // Ensure motif is within length constraint (max 2000 characters)
        if (motif.length() > 2000) {
            motif = motif.substring(0, 2000);
        }
        
        // Generate random dates
        LocalDateTime dateDemande = generateRandomDateTime(random, 2020, 2025);
        LocalDateTime dateValidation = random.nextBoolean() ? 
            generateRandomDateTime(random, 2020, 2025) : null;
        
        // Generate random status
        StatutDerogation statut = random.choose(StatutDerogation.values());
        
        // Generate random validator ID (may be null for EN_ATTENTE status)
        Long validateurId = (statut != StatutDerogation.EN_ATTENTE && random.nextBoolean()) ? 
            random.nextLong(1, 1000) : null;
        
        // Generate random validation comment (may be null)
        String commentaireValidation = (validateurId != null && random.nextBoolean()) ? 
            "Commentaire de validation " + random.nextInt(1, 100) : null;
        
        // Ensure comment is within length constraint (max 1000 characters)
        if (commentaireValidation != null && commentaireValidation.length() > 1000) {
            commentaireValidation = commentaireValidation.substring(0, 1000);
        }
        
        // Generate random justification documents (may be null)
        byte[] documentsJustificatifs = random.nextBoolean() ? 
            generateRandomDocumentBytes(random) : null;
        
        // Create a minimal inscription for the derogation
        Inscription inscription = Inscription.builder()
            .id(random.nextLong(1, 100000))
            .build();
        
        return DerogationRequest.builder()
            .id(random.nextLong(1, 100000))
            .inscription(inscription)
            .motif(motif)
            .dateDemande(dateDemande)
            .statut(statut)
            .validateurId(validateurId)
            .commentaireValidation(commentaireValidation)
            .dateValidation(dateValidation)
            .documentsJustificatifs(documentsJustificatifs)
            .build();
    }
    
    /**
     * Generates a random LocalDateTime between the given years.
     */
    private LocalDateTime generateRandomDateTime(SourceOfRandomness random, int startYear, int endYear) {
        int year = random.nextInt(startYear, endYear + 1);
        int month = random.nextInt(1, 13);
        int day = random.nextInt(1, 29); // Safe day for all months
        int hour = random.nextInt(0, 24);
        int minute = random.nextInt(0, 60);
        
        return LocalDateTime.of(year, month, day, hour, minute);
    }
    
    /**
     * Generates random document bytes with various sizes.
     */
    private byte[] generateRandomDocumentBytes(SourceOfRandomness random) {
        // Generate documents between 1KB and 5MB
        int size = random.nextInt(1024, 5 * 1024 * 1024);
        byte[] bytes = new byte[size];
        random.nextBytes(bytes);
        return bytes;
    }
}
