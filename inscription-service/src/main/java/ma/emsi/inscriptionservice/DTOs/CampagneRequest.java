package ma.emsi.inscriptionservice.DTOs;

import jakarta.validation.constraints.FutureOrPresent;
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
    @FutureOrPresent(message = "La date de début doit être dans le futur ou aujourd'hui")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est requise")
    private LocalDate dateFin;

    @NotNull(message = "L'année universitaire est requise")
    private Integer anneeUniversitaire;
}
