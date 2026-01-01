package ma.emsi.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record ProfileCompleteRequest(
        @NotNull(message = "Date de naissance est obligatoire") LocalDate dateNaissance,

        @NotBlank(message = "Lieu de naissance est obligatoire") String lieuNaissance,

        @NotBlank(message = "Nationalité est obligatoire") String nationalite,

        @NotBlank(message = "CIN est obligatoire") @Pattern(regexp = "^[A-Z]{1,2}\\d{5,6}$", message = "Format CIN invalide (ex: A123456 ou AB123456)") String cin,

        String photoUrl) {
}
