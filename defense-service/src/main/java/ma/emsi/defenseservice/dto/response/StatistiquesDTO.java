package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.enums.Mention;

import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesDTO {
    private Map<DefenseRequestStatus, Long> demandesParStatut;
    private List<DefenseCountByMonthDTO> soutenancesParMois;
    private Map<Mention, Long> mentionsDistribuees;
    private double tauxReussiteGlobal;
    private double dureeMoyenneSoutenance; // in years
}
