package ma.emsi.inscriptionservice.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import ma.emsi.inscriptionservice.entities.Campagne;
import ma.emsi.inscriptionservice.enums.TypeCampagne;

import java.time.LocalDate;

/**
 * Custom generator for Campagne entities for property-based testing.
 * Generates random campaigns with valid and edge-case data.
 */
public class CampagneGenerator extends Generator<Campagne> {

    private static final String[] LIBELLE_TEMPLATES = {
        "Campagne d'inscription",
        "Campagne de réinscription",
        "Inscription doctorale",
        "Réinscription doctorale",
        "Campagne inscription PhD",
        "Campagne réinscription PhD"
    };

    public CampagneGenerator() {
        super(Campagne.class);
    }

    @Override
    public Campagne generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate random type
        TypeCampagne type = random.choose(TypeCampagne.values());
        
        // Generate random academic year
        Integer anneeUniversitaire = random.nextInt(2020, 2026);
        
        // Generate libelle based on type and year
        String libelleTemplate = random.choose(LIBELLE_TEMPLATES);
        String libelle = libelleTemplate + " " + anneeUniversitaire + "-" + (anneeUniversitaire + 1);
        
        // Generate random dates with various scenarios
        LocalDate[] dates = generateCampaignDates(random, anneeUniversitaire);
        LocalDate dateDebut = dates[0];
        LocalDate dateFin = dates[1];
        
        // Generate random active flag
        Boolean active = random.nextBoolean();
        
        return Campagne.builder()
            .id(random.nextLong(1, 10000))
            .libelle(libelle)
            .type(type)
            .dateDebut(dateDebut)
            .dateFin(dateFin)
            .active(active)
            .anneeUniversitaire(anneeUniversitaire)
            .build();
    }
    
    /**
     * Generates campaign dates with various edge cases:
     * - Normal campaigns (2-3 months duration)
     * - Short campaigns (1 week - 1 month)
     * - Long campaigns (6+ months)
     * - Past campaigns
     * - Current campaigns
     * - Future campaigns
     * - Campaigns with dateDebut = dateFin (edge case)
     * - Campaigns with dateFin before dateDebut (invalid, for testing)
     */
    private LocalDate[] generateCampaignDates(SourceOfRandomness random, int year) {
        int scenario = random.nextInt(0, 100);
        
        LocalDate dateDebut;
        LocalDate dateFin;
        
        if (scenario < 40) {
            // Normal campaigns (40% chance) - 2-3 months duration
            int startMonth = random.nextInt(1, 11); // Jan to Oct
            int startDay = random.nextInt(1, 29);
            dateDebut = LocalDate.of(year, startMonth, startDay);
            
            int durationDays = random.nextInt(60, 91); // 2-3 months
            dateFin = dateDebut.plusDays(durationDays);
            
        } else if (scenario < 60) {
            // Short campaigns (20% chance) - 1 week to 1 month
            int startMonth = random.nextInt(1, 12);
            int startDay = random.nextInt(1, 29);
            dateDebut = LocalDate.of(year, startMonth, startDay);
            
            int durationDays = random.nextInt(7, 31); // 1 week to 1 month
            dateFin = dateDebut.plusDays(durationDays);
            
        } else if (scenario < 75) {
            // Long campaigns (15% chance) - 6+ months
            int startMonth = random.nextInt(1, 7); // Jan to Jun
            int startDay = random.nextInt(1, 29);
            dateDebut = LocalDate.of(year, startMonth, startDay);
            
            int durationDays = random.nextInt(180, 365); // 6-12 months
            dateFin = dateDebut.plusDays(durationDays);
            
        } else if (scenario < 85) {
            // Past campaigns (10% chance)
            int startMonth = random.nextInt(1, 12);
            int startDay = random.nextInt(1, 29);
            dateDebut = LocalDate.of(year - 1, startMonth, startDay);
            
            int durationDays = random.nextInt(60, 91);
            dateFin = dateDebut.plusDays(durationDays);
            
        } else if (scenario < 95) {
            // Future campaigns (10% chance)
            int startMonth = random.nextInt(1, 12);
            int startDay = random.nextInt(1, 29);
            dateDebut = LocalDate.of(year + 1, startMonth, startDay);
            
            int durationDays = random.nextInt(60, 91);
            dateFin = dateDebut.plusDays(durationDays);
            
        } else if (scenario < 98) {
            // Same day campaigns (3% chance) - edge case
            int startMonth = random.nextInt(1, 12);
            int startDay = random.nextInt(1, 29);
            dateDebut = LocalDate.of(year, startMonth, startDay);
            dateFin = dateDebut;
            
        } else {
            // Invalid campaigns (2% chance) - dateFin before dateDebut
            int startMonth = random.nextInt(2, 12);
            int startDay = random.nextInt(1, 29);
            dateDebut = LocalDate.of(year, startMonth, startDay);
            
            int endMonth = random.nextInt(1, startMonth);
            int endDay = random.nextInt(1, 29);
            dateFin = LocalDate.of(year, endMonth, endDay);
        }
        
        return new LocalDate[]{dateDebut, dateFin};
    }
}
