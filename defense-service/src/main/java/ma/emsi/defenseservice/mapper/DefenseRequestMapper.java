package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.dto.request.DefenseRequestCreateDTO;
import ma.emsi.defenseservice.dto.response.DefenseRequestResponseDTO;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.service.UserServiceFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefenseRequestMapper {

    @Autowired
    private UserServiceFacade userServiceFacade;

    public DefenseRequest toEntity(DefenseRequestCreateDTO dto) {
        DefenseRequest entity = new DefenseRequest();
        entity.setDoctorantId(dto.getDoctorantId());
        // Ne pas lier Prerequisites ici, le service le fera
        return entity;
    }

    public DefenseRequestResponseDTO toDTO(DefenseRequest entity) {
        DefenseRequestResponseDTO dto = new DefenseRequestResponseDTO();
        dto.setId(entity.getId());
        dto.setDoctorantId(entity.getDoctorantId());
        dto.setSubmissionDate(entity.getSubmissionDate());
        dto.setStatus(entity.getStatus());
        dto.setRejectionReason(entity.getRejectionReason());

        // ✅ Enrichir avec les données du doctorant depuis user-service (avec
        // Resilience4j)
        UserDTO user = userServiceFacade.getUserById(entity.getDoctorantId());
        dto.setDoctorantFirstName(user.getFirstName());
        dto.setDoctorantLastName(user.getLastName());
        dto.setDoctorantEmail(user.getEmail());

        // Relations simplifiées
        if (entity.getPrerequisites() != null) {
            dto.setPrerequisitesId(entity.getPrerequisites().getId());
        }
        if (entity.getJury() != null) {
            dto.setJuryId(entity.getJury().getId());
        }
        if (entity.getDefense() != null) {
            dto.setDefenseId(entity.getDefense().getId());
        }
        if (entity.getDocuments() != null) {
            dto.setDocumentsCount(entity.getDocuments().size());
        }
        if (entity.getRapports() != null) {
            dto.setRapportsCount(entity.getRapports().size());
        }

        return dto;
    }
}
