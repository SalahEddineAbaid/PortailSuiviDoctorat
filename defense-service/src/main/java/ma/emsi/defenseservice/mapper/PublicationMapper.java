package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.request.PublicationCreateDTO;
import ma.emsi.defenseservice.dto.response.PublicationResponseDTO;
import ma.emsi.defenseservice.entity.Publication;
import org.springframework.stereotype.Component;

@Component
public class PublicationMapper {

    public Publication toEntity(PublicationCreateDTO dto) {
        Publication entity = new Publication();
        entity.setTitre(dto.getTitre());
        entity.setJournal(dto.getJournal());
        entity.setAnneePublication(dto.getAnneePublication());
        entity.setType(dto.getType());
        entity.setQuartile(dto.getQuartile());
        entity.setDoi(dto.getDoi());
        entity.setUrl(dto.getUrl());
        // Prerequisites will be set by the service layer
        // Validation fields default to false/null as per entity defaults
        return entity;
    }

    public PublicationResponseDTO toDTO(Publication entity) {
        PublicationResponseDTO dto = new PublicationResponseDTO();
        dto.setId(entity.getId());
        dto.setTitre(entity.getTitre());
        dto.setJournal(entity.getJournal());
        dto.setAnneePublication(entity.getAnneePublication());
        dto.setType(entity.getType());
        dto.setQuartile(entity.getQuartile());
        dto.setDoi(entity.getDoi());
        dto.setUrl(entity.getUrl());

        // Validation fields
        dto.setValide(entity.isValide());
        dto.setValidateurId(entity.getValidateurId());
        dto.setCommentaireValidation(entity.getCommentaireValidation());
        dto.setDateValidation(entity.getDateValidation());

        // Prerequisites relationship
        if (entity.getPrerequisites() != null) {
            dto.setPrerequisitesId(entity.getPrerequisites().getId());
        }

        return dto;
    }
}
