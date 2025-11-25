package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefenseRequestCreateDTO {

    @NotNull(message = "L'ID du doctorant est requis")
    private Long doctorantId;

    private Long prerequisitesId;
}
