package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutorisationDTO {

    @NotNull(message = "L'ID de l'administrateur est requis")
    private Long administrateurId;

    @NotNull(message = "La date de soutenance est requise")
    @Future(message = "La date de soutenance doit être dans le futur")
    private LocalDateTime dateSoutenance;

    @NotBlank(message = "Le lieu de soutenance est requis")
    private String lieuSoutenance;

    @NotBlank(message = "La salle de soutenance est requise")
    private String salleSoutenance;

    private String commentaireAdmin;
}
