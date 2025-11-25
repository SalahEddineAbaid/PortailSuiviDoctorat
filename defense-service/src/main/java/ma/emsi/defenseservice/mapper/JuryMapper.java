package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.request.JuryCreateDTO;
import ma.emsi.defenseservice.dto.response.JuryMemberResponseDTO;
import ma.emsi.defenseservice.dto.response.JuryResponseDTO;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.entity.Jury;
import ma.emsi.defenseservice.entity.JuryMember;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class JuryMapper {

    public Jury toEntity(JuryCreateDTO dto) {
        Jury entity = new Jury();
        entity.setDirectorId(dto.getDirectorId());

        DefenseRequest defenseRequest = new DefenseRequest();
        defenseRequest.setId(dto.getDefenseRequestId());
        entity.setDefenseRequest(defenseRequest);

        return entity;
    }

    public JuryResponseDTO toDTO(Jury entity) {
        JuryResponseDTO dto = new JuryResponseDTO();
        dto.setId(entity.getId());
        dto.setDirectorId(entity.getDirectorId());
        dto.setStatus(entity.getStatus());
        dto.setProposalDate(entity.getProposalDate());
        dto.setValidationDate(entity.getValidationDate());

        if (entity.getDefenseRequest() != null) {
            dto.setDefenseRequestId(entity.getDefenseRequest().getId());
        }

        if (entity.getMembers() != null) {
            dto.setMembers(entity.getMembers().stream()
                    .map(this::memberToDTO)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private JuryMemberResponseDTO memberToDTO(JuryMember member) {
        JuryMemberResponseDTO dto = new JuryMemberResponseDTO();
        dto.setId(member.getId());
        dto.setName(member.getName());
        dto.setEmail(member.getEmail());
        dto.setAffiliation(member.getAffiliation());
        dto.setGrade(member.getGrade());
        dto.setRole(member.getRole());
        dto.setStatus(member.getStatus());
        return dto;
    }
}
