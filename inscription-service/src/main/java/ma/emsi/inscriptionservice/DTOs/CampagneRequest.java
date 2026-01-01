package ma.emsi.inscriptionservice.DTOs;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ma.emsi.inscriptionservice.enums.TypeCampagne;

import java.time.LocalDate;

@Data
public class CampagneRequest {

    @NotBlank(message = "Le libellé est requis")
    private String libelle;

    @NotNull(message = "Le type de campagne est requis")
    private TypeCampagne type;

    @NotNull(message = "La date de début est requise")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est requise")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFin;

    @NotNull(message = "L'année universitaire est requise")
    private Integer anneeUniversitaire;
}
