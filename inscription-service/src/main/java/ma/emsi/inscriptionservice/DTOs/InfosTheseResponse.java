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
public class InfosTheseResponse {
    private Long id;
    private String titreThese;
    private String discipline;
    private String laboratoire;
    private String etablissementAccueil;
    private Boolean cotutelle;
    private String universitePartenaire;
    private String paysPartenaire;
    private LocalDate dateDebutPrevue;
}
