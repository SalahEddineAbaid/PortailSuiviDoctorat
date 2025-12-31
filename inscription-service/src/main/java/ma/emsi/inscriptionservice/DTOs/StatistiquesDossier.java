package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesDossier {
    private Double tauxCompletionDossier;
    private Integer documentsValides;
    private Integer documentsTotal;
}
