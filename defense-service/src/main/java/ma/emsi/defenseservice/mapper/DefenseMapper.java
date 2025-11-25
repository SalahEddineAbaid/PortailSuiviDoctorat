package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.request.DefenseScheduleDTO;
import ma.emsi.defenseservice.dto.response.DefenseResponseDTO;
import ma.emsi.defenseservice.entity.Defense;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.enums.DefenseStatus;
import org.springframework.stereotype.Component;

@Component
public class DefenseMapper {

    public Defense toEntity(DefenseScheduleDTO dto) {
        Defense entity = new Defense();
        entity.setDefenseDate(dto.getDefenseDate());
        entity.setLocation(dto.getLocation());
        entity.setRoom(dto.getRoom());
        entity.setStatus(DefenseStatus.SCHEDULED);

        DefenseRequest defenseRequest = new DefenseRequest();
        defenseRequest.setId(dto.getDefenseRequestId());
        entity.setDefenseRequest(defenseRequest);

        return entity;
    }

    public DefenseResponseDTO toDTO(Defense entity) {
        DefenseResponseDTO dto = new DefenseResponseDTO();
        dto.setId(entity.getId());
        dto.setDefenseDate(entity.getDefenseDate());
        dto.setLocation(entity.getLocation());
        dto.setRoom(entity.getRoom());
        dto.setStatus(entity.getStatus());
        dto.setProcesVerbalUrl(entity.getProcesVerbalUrl());
        dto.setMention(entity.getMention());
        dto.setPublicationRecommended(entity.isPublicationRecommended());
        dto.setJuryComments(entity.getJuryComments());
        dto.setDeliberationDate(entity.getDeliberationDate());

        if (entity.getDefenseRequest() != null) {
            dto.setDefenseRequestId(entity.getDefenseRequest().getId());
        }

        return dto;
    }
}
