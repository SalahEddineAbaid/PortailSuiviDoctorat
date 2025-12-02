package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.dto.request.JuryMemberCreateDTO;
import ma.emsi.defenseservice.dto.response.JuryMemberResponseDTO;
import ma.emsi.defenseservice.entity.Jury;
import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.enums.MemberStatus;
import ma.emsi.defenseservice.service.UserServiceFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JuryMemberMapper {

    @Autowired
    private UserServiceFacade userServiceFacade;

    public JuryMember toEntity(JuryMemberCreateDTO dto) {
        JuryMember entity = new JuryMember();
        entity.setProfessorId(dto.getProfessorId());
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
        dto.setProfessorId(entity.getProfessorId());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());

        // ✅ Enrichissement depuis user-service (avec Resilience4j)
        UserDTO professor = userServiceFacade.getUserById(entity.getProfessorId());
        dto.setProfessorName(professor.getFirstName() + " " + professor.getLastName());
        dto.setProfessorEmail(professor.getEmail());
        dto.setProfessorPhone(professor.getPhoneNumber());

        return dto;
    }
}
