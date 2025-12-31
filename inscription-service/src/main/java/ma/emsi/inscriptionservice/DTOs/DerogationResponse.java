package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.StatutDerogation;

import java.time.LocalDateTime;

/**
 * DTO for returning derogation request information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DerogationResponse {

    private Long id;
    private Long inscriptionId;
    private String motif;
    private StatutDerogation statut;
    private LocalDateTime dateDemande;
    private Long validateurId;
    private String commentaireValidation;
    private LocalDateTime dateValidation;
    private boolean hasDocuments;
}
