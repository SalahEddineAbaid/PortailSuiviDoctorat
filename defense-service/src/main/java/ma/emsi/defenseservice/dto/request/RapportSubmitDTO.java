package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapportSubmitDTO {

    @NotBlank(message = "L'URL du rapport est requise")
    private String reportUrl;

    @NotNull(message = "L'avis est requis")
    private Boolean favorable;

    @NotNull(message = "L'ID de la demande est requis")
    private Long defenseRequestId;

    @NotNull(message = "L'ID du membre du jury est requis")
    private Long juryMemberId;
}
