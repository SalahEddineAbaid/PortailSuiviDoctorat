package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.MemberRole;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JuryMemberCreateDTO {

    @NotNull(message = "L'ID du professeur est requis")
    private Long professorId; // ✅ Référence au user-service

    @NotNull(message = "Le rôle est requis")
    private MemberRole role;

    @NotNull(message = "L'ID du jury est requis")
    private Long juryId;
}
