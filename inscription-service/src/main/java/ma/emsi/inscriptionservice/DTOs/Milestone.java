package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Milestone {
    private String type;
    private LocalDate dateEcheance;
    private String statut;
}
