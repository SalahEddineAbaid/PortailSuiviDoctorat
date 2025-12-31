package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.enums.TypeInscription;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionHistorique {
    private Long id;
    private Integer annee;
    private TypeInscription type;
    private StatutInscription statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;
}
