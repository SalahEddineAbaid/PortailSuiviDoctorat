package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.enums.TypeInscription;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionCourante {
    private Long id;
    private Integer annee;
    private TypeInscription type;
    private StatutInscription statut;
    private Long dureeDoctorat;
    private Boolean derogationActive;
}
