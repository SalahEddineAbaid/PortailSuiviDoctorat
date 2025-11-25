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
    private String name;
    private String email;
    private String affiliation;
    private String grade;
    private MemberRole role;
    private MemberStatus status;
}
