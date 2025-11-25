package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.MemberRole;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JuryMemberCreateDTO {

    @NotBlank(message = "Le nom est requis")
    private String name;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "L'affiliation est requise")
    private String affiliation;

    @NotBlank(message = "Le grade est requis")
    private String grade;

    @NotNull(message = "Le rôle est requis")
    private MemberRole role;

    @NotNull(message = "L'ID du jury est requis")
    private Long juryId;
}
