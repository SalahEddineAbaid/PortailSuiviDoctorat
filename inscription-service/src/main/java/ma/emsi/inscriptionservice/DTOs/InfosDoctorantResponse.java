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
public class InfosDoctorantResponse {
    private Long id;
    private String cin;
    private String cne;
    private String telephone;
    private String adresse;
    private String ville;
    private String pays;
    private LocalDate dateNaissance;
    private String lieuNaissance;
    private String nationalite;
}
