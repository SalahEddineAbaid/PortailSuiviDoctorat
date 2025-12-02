package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.client.UserServiceClient;
import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.dto.request.RapportSubmitDTO;
import ma.emsi.defenseservice.dto.response.RapportResponseDTO;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.entity.Rapport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RapportMapper {

    @Autowired
    private UserServiceClient userServiceClient;

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

            // ✅ Enrichissement avec les données du professeur depuis user-service
            try {
                UserDTO professor = userServiceClient.getUserById(entity.getJuryMember().getProfessorId());
                dto.setJuryMemberName(professor.getFirstName() + " " + professor.getLastName());
            } catch (Exception e) {
                // En cas d'erreur, on laisse le champ null
                System.err.println("⚠️ Impossible d'enrichir le rapport " + entity.getId() +
                        " avec les données du professeur " + entity.getJuryMember().getProfessorId());
            }
        }

        return dto;
    }
}
