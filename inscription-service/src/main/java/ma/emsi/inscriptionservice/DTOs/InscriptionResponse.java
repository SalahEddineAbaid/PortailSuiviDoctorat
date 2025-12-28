package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.enums.TypeInscription;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionResponse {

    private Long id;
    private Long doctorantId;
    private Long directeurTheseId;
    private String sujetThese;
    private TypeInscription type;
    private Integer anneeInscription;
    private StatutInscription statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;
    private Long dureeDoctorat;
    private Boolean derogation;
    private String motifDerogation;
    private String commentaireDirecteur;
    private String commentaireAdmin;

    private InfosDoctorantResponse infosDoctorant;
    private InfosTheseResponse infosThese;
    private List<DocumentResponse> documents;
    private List<ValidationResponse> validations;
}
