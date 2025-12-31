package ma.emsi.inscriptionservice.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import ma.emsi.inscriptionservice.entities.Campagne;
import ma.emsi.inscriptionservice.entities.Inscription;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.enums.TypeInscription;

import java.time.LocalDateTime;

/**
 * Custom generator for Inscription entities for property-based testing.
 * Generates random inscriptions with valid and edge-case data.
 */
public class InscriptionGenerator extends Generator<Inscription> {

    public InscriptionGenerator() {
        super(Inscription.class);
    }

    @Override
    public Inscription generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate random IDs
        Long doctorantId = random.nextLong(1, 10000);
        Long directeurTheseId = random.nextLong(1, 1000);
        
        // Generate random type and status
        TypeInscription type = random.choose(TypeInscription.values());
        StatutInscription statut = random.choose(StatutInscription.values());
        
        // Generate random year between 2020 and 2025
        Integer anneeInscription = random.nextInt(2020, 2026);
        
        // Generate random dates
        LocalDateTime dateCreation = generateRandomDateTime(random, 2020, 2025);
        LocalDateTime datePremiereInscription = generateRandomDateTime(random, 2018, 2024);
        
        // Generate random validation date (may be null)
        LocalDateTime dateValidation = random.nextBoolean() ? 
            generateRandomDateTime(random, 2020, 2025) : null;
        
        // Generate random derogation flag
        Boolean derogation = random.nextBoolean();
        
        // Generate random blocking flag
        boolean bloqueReInscription = random.nextBoolean();
        
        // Generate random thesis subject
        String sujetThese = "Sujet de thèse " + random.nextInt(1, 1000);
        
        // Generate random comments (may be null)
        String commentaireDirecteur = random.nextBoolean() ? 
            "Commentaire directeur " + random.nextInt(1, 100) : null;
        String commentaireAdmin = random.nextBoolean() ? 
            "Commentaire admin " + random.nextInt(1, 100) : null;
        String motifDerogation = derogation && random.nextBoolean() ? 
            "Motif dérogation " + random.nextInt(1, 100) : null;
        
        // Create a minimal campagne for the inscription
        Campagne campagne = Campagne.builder()
            .id(random.nextLong(1, 100))
            .build();
        
        return Inscription.builder()
            .id(random.nextLong(1, 100000))
            .doctorantId(doctorantId)
            .directeurTheseId(directeurTheseId)
            .campagne(campagne)
            .sujetThese(sujetThese)
            .type(type)
            .anneeInscription(anneeInscription)
            .statut(statut)
            .dateCreation(dateCreation)
            .dateValidation(dateValidation)
            .datePremiereInscription(datePremiereInscription)
            .commentaireDirecteur(commentaireDirecteur)
            .commentaireAdmin(commentaireAdmin)
            .derogation(derogation)
            .motifDerogation(motifDerogation)
            .bloqueReInscription(bloqueReInscription)
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
}
