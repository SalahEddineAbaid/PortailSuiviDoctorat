package ma.emsi.inscriptionservice.exceptions;

import java.time.LocalDate;

/**
 * Exception thrown when a user attempts to register in a closed campaign.
 */
public class CampagneClosedException extends RuntimeException {
    
    private final Long campagneId;
    private final String libelle;
    private final LocalDate dateFin;
    
    public CampagneClosedException(Long campagneId, String libelle, LocalDate dateFin) {
        super(String.format(
            "La campagne '%s' est fermée depuis le %s. " +
            "Les inscriptions ne sont plus acceptées.",
            libelle, dateFin
        ));
        this.campagneId = campagneId;
        this.libelle = libelle;
        this.dateFin = dateFin;
    }
    
    public CampagneClosedException(String message) {
        super(message);
        this.campagneId = null;
        this.libelle = null;
        this.dateFin = null;
    }
    
    public Long getCampagneId() {
        return campagneId;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    public LocalDate getDateFin() {
        return dateFin;
    }
}
