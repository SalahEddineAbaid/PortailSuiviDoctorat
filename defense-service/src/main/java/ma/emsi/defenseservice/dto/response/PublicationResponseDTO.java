package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.QuartileJournal;
import ma.emsi.defenseservice.enums.TypePublication;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicationResponseDTO {

    private Long id;
    private Long prerequisitesId;
    private String titre;
    private String journal;
    private Integer anneePublication;
    private TypePublication type;
    private QuartileJournal quartile;
    private String doi;
    private String url;

    // Validation fields
    private boolean valide;
    private Long validateurId;
    private String commentaireValidation;
    private LocalDateTime dateValidation;
}
