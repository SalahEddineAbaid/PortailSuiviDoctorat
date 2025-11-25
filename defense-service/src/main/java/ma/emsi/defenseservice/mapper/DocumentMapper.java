package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.request.DocumentUploadDTO;
import ma.emsi.defenseservice.dto.response.DocumentResponseDTO;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.entity.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public Document toEntity(DocumentUploadDTO dto) {
        Document entity = new Document();
        entity.setFileName(dto.getFileName());
        entity.setFileType(dto.getFileType());
        entity.setFileUrl(dto.getFileUrl());
        entity.setType(dto.getType());

        DefenseRequest defenseRequest = new DefenseRequest();
        defenseRequest.setId(dto.getDefenseRequestId());
        entity.setDefenseRequest(defenseRequest);

        return entity;
    }

    public DocumentResponseDTO toDTO(Document entity) {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setId(entity.getId());
        dto.setFileName(entity.getFileName());
        dto.setFileType(entity.getFileType());
        dto.setFileUrl(entity.getFileUrl());
        dto.setType(entity.getType());
        dto.setUploadDate(entity.getUploadDate());
        return dto;
    }
}
