package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesDirecteurDTO {
    private int doctorantsActifs;
    private int soutenancesAPlanifier;
    private int rapportsEnAttente;
    private int jurysAProposer;
}
