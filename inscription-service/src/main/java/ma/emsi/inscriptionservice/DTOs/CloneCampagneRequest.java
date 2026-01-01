package ma.emsi.inscriptionservice.DTOs;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CloneCampagneRequest {

    @NotNull(message = "La date de début est requise")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est requise")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;
}
