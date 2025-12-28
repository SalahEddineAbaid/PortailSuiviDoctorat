package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.request.PrerequisitesCreateDTO;
import ma.emsi.defenseservice.dto.response.PrerequisitesResponseDTO;
import ma.emsi.defenseservice.entity.Prerequisites;
import org.springframework.stereotype.Component;

@Component
public class PrerequisitesMapper {

    public Prerequisites toEntity(PrerequisitesCreateDTO dto) {
        Prerequisites entity = new Prerequisites();
        entity.setDoctorantId(dto.getDoctorantId());
        entity.setJournalArticles(dto.getJournalArticles());
        entity.setConferences(dto.getConferences());
        entity.setTrainingHours(dto.getTrainingHours());
        entity.setManuscriptUploaded(dto.isManuscriptUploaded());
        entity.setAntiPlagiarismUploaded(dto.isAntiPlagiarismUploaded());
        entity.setPublicationsReportUploaded(dto.isPublicationsReportUploaded());
        entity.setTrainingCertsUploaded(dto.isTrainingCertsUploaded());
        entity.setAuthorizationLetterUploaded(dto.isAuthorizationLetterUploaded());
        entity.setValid(false); // Par défaut non validé
        return entity;
    }

    public PrerequisitesResponseDTO toDTO(Prerequisites entity) {
        PrerequisitesResponseDTO dto = new PrerequisitesResponseDTO();
        dto.setId(entity.getId());
        dto.setDoctorantId(entity.getDoctorantId());
        dto.setJournalArticles(entity.getJournalArticles());
        dto.setConferences(entity.getConferences());
        dto.setTrainingHours(entity.getTrainingHours());
        dto.setManuscriptUploaded(entity.isManuscriptUploaded());
        dto.setAntiPlagiarismUploaded(entity.isAntiPlagiarismUploaded());
        dto.setPublicationsReportUploaded(entity.isPublicationsReportUploaded());
        dto.setTrainingCertsUploaded(entity.isTrainingCertsUploaded());
        dto.setAuthorizationLetterUploaded(entity.isAuthorizationLetterUploaded());
        dto.setValid(entity.isValid());

        // ✅ Enrichissement avec les données du doctorant (optionnel, peut être fait
        // dans le controller)

        return dto;
    }
}
