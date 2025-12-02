package ma.emsi.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.Set;

public record RegisterRequest(
                @Email(message = "Email invalide") @NotBlank String email,

                @NotBlank(message = "Le mot de passe ne peut pas être vide") @Size(min = 12, max = 64, message = "Le mot de passe doit contenir entre 12 et 64 caractères") @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.])[A-Za-z\\d@$!%*?&.]{12,}$", message = "Le mot de passe doit contenir au moins une majuscule, une minuscule, un chiffre et un caractère spécial") String password,
                @NotBlank(message = "Veuillez saisir votre nom") String firstName,
                @NotBlank(message = "Veuillez saisir votre prenom") String lastName,

                @NotBlank(message = "Le numéro de téléphone est obligatoire") String phoneNumber,
                @NotBlank(message = "L'adresse est obligatoire") String adresse,
                @NotBlank(message = "La ville est obligatoire") String ville,
                @NotBlank(message = "Le pays est obligatoire") String pays,

                // ✅ AJOUT : Champ pour les rôles (optionnel)
                Set<String> roles) {
}
