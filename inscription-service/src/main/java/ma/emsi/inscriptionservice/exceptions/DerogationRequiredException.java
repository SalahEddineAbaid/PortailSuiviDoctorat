package ma.emsi.inscriptionservice.exceptions;

/**
 * Exception thrown when a student attempts re-registration with duration exceeding 3 years
 * without an approved derogation.
 */
public class DerogationRequiredException extends RuntimeException {
    
    private final Long inscriptionId;
    private final Long doctorantId;
    private final double dureeAnnees;
    
    public DerogationRequiredException(Long inscriptionId, Long doctorantId, double dureeAnnees) {
        super(String.format(
            "Une dérogation approuvée est requise pour la réinscription. " +
            "Durée actuelle: %.1f ans (limite: 3 ans). " +
            "Veuillez soumettre une demande de dérogation.",
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
