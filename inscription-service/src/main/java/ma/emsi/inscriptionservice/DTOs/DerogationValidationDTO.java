package ma.emsi.inscriptionservice.DTOs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for validating a derogation request (by director or PED).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DerogationValidationDTO {

    @NotNull(message = "La décision est requise")
    private Boolean approuve;

    @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères")
    private String commentaire;
}
