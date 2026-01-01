package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefusDTO {

    @NotNull(message = "L'ID de l'administrateur est requis")
    private Long administrateurId;

    @NotBlank(message = "Le motif de refus est requis")
    private String motifRefus;

    private String commentaireAdmin;
}
