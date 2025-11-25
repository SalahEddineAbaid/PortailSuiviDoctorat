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
public class DefenseScheduleDTO {

    @NotNull(message = "La date de soutenance est requise")
    @Future(message = "La date doit être dans le futur")
    private LocalDateTime defenseDate;

    @NotBlank(message = "Le lieu est requis")
    private String location;

    @NotBlank(message = "La salle est requise")
    private String room;

    @NotNull(message = "L'ID de la demande est requis")
    private Long defenseRequestId;
}
