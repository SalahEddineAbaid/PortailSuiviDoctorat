package ma.emsi.inscriptionservice.exceptions;

/**
 * Exception thrown when a student exceeds the 6-year duration limit.
 * This blocks re-registration permanently.
 */
public class DurationLimitExceededException extends RuntimeException {
    
    private final Long inscriptionId;
    private final Long doctorantId;
    private final double dureeAnnees;
    
    public DurationLimitExceededException(Long inscriptionId, Long doctorantId, double dureeAnnees) {
        super(String.format(
            "La durée maximale de 6 ans est dépassée (%.1f ans). " +
            "Réinscription impossible. Veuillez contacter l'administration.",
            dureeAnnees
        ));
        this.inscriptionId = inscriptionId;
        this.doctorantId = doctorantId;
        this.dureeAnnees = dureeAnnees;
    }
    
    public Long getInscriptionId() {
        return inscriptionId;
    }
    
    public Long getDoctorantId() {
        return doctorantId;
    }
    
    public double getDureeAnnees() {
        return dureeAnnees;
    }
}
