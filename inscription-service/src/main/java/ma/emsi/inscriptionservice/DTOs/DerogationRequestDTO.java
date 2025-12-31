package ma.emsi.inscriptionservice.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a derogation request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DerogationRequestDTO {

    @NotBlank(message = "Le motif de la dérogation est obligatoire")
    @Size(min = 50, max = 2000, message = "Le motif doit contenir entre 50 et 2000 caractères")
    private String motif;

    // Documents justificatifs will be handled as MultipartFile in the controller
    // and converted to byte[] before passing to service
}
