package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.dto.request.RapportSubmitDTO;
import ma.emsi.defenseservice.dto.response.RapportResponseDTO;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.entity.Rapport;
import ma.emsi.defenseservice.service.UserServiceFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RapportMapper {

    @Autowired
    private UserServiceFacade userServiceFacade;

    public Rapport toEntity(RapportSubmitDTO dto) {
        Rapport entity = new Rapport();
        entity.setReportUrl(dto.getReportUrl());
        entity.setFavorable(dto.getFavorable());

        DefenseRequest defenseRequest = new DefenseRequest();
        defenseRequest.setId(dto.getDefenseRequestId());
        entity.setDefenseRequest(defenseRequest);

        JuryMember juryMember = new JuryMember();
        juryMember.setId(dto.getJuryMemberId());
        entity.setJuryMember(juryMember);

        return entity;
    }

    public RapportResponseDTO toDTO(Rapport entity) {
        RapportResponseDTO dto = new RapportResponseDTO();
        dto.setId(entity.getId());
        dto.setReportUrl(entity.getReportUrl());
        dto.setFavorable(entity.isFavorable());
        dto.setSubmissionDate(entity.getSubmissionDate());

        if (entity.getDefenseRequest() != null) {
            dto.setDefenseRequestId(entity.getDefenseRequest().getId());
        }

        if (entity.getJuryMember() != null) {
            dto.setJuryMemberId(entity.getJuryMember().getId());

            // ✅ Enrichissement avec les données du professeur depuis user-service (avec
            // Resilience4j)
            UserDTO professor = userServiceFacade.getUserById(entity.getJuryMember().getProfessorId());
            dto.setJuryMemberName(professor.getFirstName() + " " + professor.getLastName());
        }

        return dto;
    }
}
