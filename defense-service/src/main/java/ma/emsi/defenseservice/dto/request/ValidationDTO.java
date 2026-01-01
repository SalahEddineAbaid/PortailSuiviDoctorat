package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.QuartileJournal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationDTO {

    @NotNull(message = "L'ID du validateur est requis")
    private Long validateurId;

    @NotNull(message = "Le quartile vérifié est requis")
    private QuartileJournal quartile;

    private String commentaireValidation;
}
