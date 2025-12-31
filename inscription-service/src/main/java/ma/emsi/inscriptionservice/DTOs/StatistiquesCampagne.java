package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.enums.TypeCampagne;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesCampagne {
    private Long campagneId;
    private String libelle;
    private TypeCampagne type;
    private int nombreInscriptions;
    private Map<StatutInscription, Integer> parStatut;
    private double tauxValidation;
    private double tempsMoyenValidation; // in days
}
