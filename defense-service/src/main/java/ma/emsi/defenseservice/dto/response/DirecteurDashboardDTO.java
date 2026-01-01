package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.dto.external.UserDTO;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DirecteurDashboardDTO {
    private UserDTO directeur;
    private StatistiquesDirecteurDTO statistiques;
    private List<DefenseRequestSummaryDTO> demandesSoutenance = new ArrayList<>();
    private List<DefenseScheduledDTO> soutenancesProgrammees = new ArrayList<>();
    private List<AlerteDTO> alertes = new ArrayList<>();
}
