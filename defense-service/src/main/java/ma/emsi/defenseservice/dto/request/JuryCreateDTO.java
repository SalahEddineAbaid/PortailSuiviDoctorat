package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JuryCreateDTO {

    @NotNull(message = "L'ID du directeur est requis")
    private Long directorId;

    @NotNull(message = "L'ID de la demande est requis")
    private Long defenseRequestId;
}
