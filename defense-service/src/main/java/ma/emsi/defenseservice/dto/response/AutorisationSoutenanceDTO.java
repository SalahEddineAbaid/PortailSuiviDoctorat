package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.StatutAutorisation;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutorisationSoutenanceDTO {

    private Long id;
    private Long defenseRequestId;
    private StatutAutorisation statut;
    private Long administrateurId;
    private LocalDateTime dateAutorisation;

    // Verification results
    private Boolean prerequisValides;
    private Boolean juryComplet;
    private Boolean rapportsFavorables;
    private Boolean documentsComplets;

    private String commentaireAdmin;
    private String motifRefus;

    // Scheduling info
    private LocalDateTime dateSoutenance;
    private String lieuSoutenance;
    private String salleSoutenance;

    // Enriched admin information
    private String administrateurFirstName;
    private String administrateurLastName;
    private String administrateurEmail;
}
