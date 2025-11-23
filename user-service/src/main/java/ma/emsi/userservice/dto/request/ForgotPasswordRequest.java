package ma.emsi.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "L'email est requis")
        @Email(message = "Email invalide")
        String email
) {}
