package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.request.JuryMemberCreateDTO;
import ma.emsi.defenseservice.dto.response.JuryMemberResponseDTO;
import ma.emsi.defenseservice.entity.Jury;
import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.enums.MemberStatus;
import org.springframework.stereotype.Component;

@Component
public class JuryMemberMapper {

    public JuryMember toEntity(JuryMemberCreateDTO dto) {
        JuryMember entity = new JuryMember();
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setAffiliation(dto.getAffiliation());
        entity.setGrade(dto.getGrade());
        entity.setRole(dto.getRole());
        entity.setStatus(MemberStatus.INVITED);

        Jury jury = new Jury();
        jury.setId(dto.getJuryId());
        entity.setJury(jury);

        return entity;
    }

    public JuryMemberResponseDTO toDTO(JuryMember entity) {
        JuryMemberResponseDTO dto = new JuryMemberResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setEmail(entity.getEmail());
        dto.setAffiliation(entity.getAffiliation());
        dto.setGrade(entity.getGrade());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
