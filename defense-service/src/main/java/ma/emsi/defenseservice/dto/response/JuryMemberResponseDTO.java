package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.MemberRole;
import ma.emsi.defenseservice.enums.MemberStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JuryMemberResponseDTO {

    private Long id;
    private Long professorId;

    // ✅ Données enrichies depuis user-service
    private String professorName;
    private String professorEmail;
    private String professorPhone;

    private MemberRole role;
    private MemberStatus status;
}
