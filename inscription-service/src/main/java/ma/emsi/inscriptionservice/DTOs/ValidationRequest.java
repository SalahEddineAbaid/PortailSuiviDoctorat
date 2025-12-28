package ma.emsi.inscriptionservice.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ValidationRequest {

    @NotNull(message = "L'identifiant de l'inscription est requis")
    private Long inscriptionId;

    @NotNull(message = "La décision est requise")
    private Boolean approuve;

    @NotBlank(message = "Le commentaire est requis")
    private String commentaire;

    // Pour les dérogations (admin seulement)
    private Boolean accordeDerogation;

    private String motifDerogation;
}
