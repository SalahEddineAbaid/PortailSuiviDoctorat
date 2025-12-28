package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.StatutValidation;
import ma.emsi.inscriptionservice.enums.TypeValidateur;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResponse {
    private Long id;
    private Long validateurId;
    private TypeValidateur typeValidateur;
    private StatutValidation statut;
    private String commentaire;
    private LocalDateTime dateValidation;
    private LocalDateTime dateCreation;
}
