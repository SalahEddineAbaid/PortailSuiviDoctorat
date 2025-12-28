package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.TypeCampagne;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampagneResponse {
    private Long id;
    private String libelle;
    private TypeCampagne type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Boolean active;
    private Integer anneeUniversitaire;
    private Boolean ouverte;
}
