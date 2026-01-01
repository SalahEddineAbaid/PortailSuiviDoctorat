package ma.emsi.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DisableAccountRequest(
        @NotBlank(message = "La raison de désactivation est obligatoire") String reason) {
}
