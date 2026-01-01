package ma.emsi.defenseservice.mapper;

import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.dto.request.AutorisationDTO;
import ma.emsi.defenseservice.dto.request.RefusDTO;
import ma.emsi.defenseservice.dto.response.AutorisationSoutenanceDTO;
import ma.emsi.defenseservice.entity.AutorisationSoutenance;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.enums.StatutAutorisation;
import ma.emsi.defenseservice.service.UserServiceFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AutorisationMapper {

    @Autowired
    private UserServiceFacade userServiceFacade;

    /**
     * Convert AutorisationDTO to AutorisationSoutenance entity for authorization
     */
    public AutorisationSoutenance toEntityForAutorisation(AutorisationDTO dto, DefenseRequest defenseRequest) {
        AutorisationSoutenance entity = new AutorisationSoutenance();
        entity.setDefenseRequest(defenseRequest);
        entity.setStatut(StatutAutorisation.AUTORISE);
        entity.setAdministrateurId(dto.getAdministrateurId());
        entity.setDateAutorisation(LocalDateTime.now());
        entity.setDateSoutenance(dto.getDateSoutenance());
        entity.setLieuSoutenance(dto.getLieuSoutenance());
        entity.setSalleSoutenance(dto.getSalleSoutenance());
        entity.setCommentaireAdmin(dto.getCommentaireAdmin());
        return entity;
    }

    /**
     * Convert RefusDTO to AutorisationSoutenance entity for refusal
     */
    public AutorisationSoutenance toEntityForRefus(RefusDTO dto, DefenseRequest defenseRequest) {
        AutorisationSoutenance entity = new AutorisationSoutenance();
        entity.setDefenseRequest(defenseRequest);
        entity.setStatut(StatutAutorisation.REFUSE);
        entity.setAdministrateurId(dto.getAdministrateurId());
        entity.setDateAutorisation(LocalDateTime.now());
        entity.setMotifRefus(dto.getMotifRefus());
        entity.setCommentaireAdmin(dto.getCommentaireAdmin());
        return entity;
    }

    /**
     * Convert AutorisationSoutenance entity to AutorisationSoutenanceDTO
     */
    public AutorisationSoutenanceDTO toDTO(AutorisationSoutenance entity) {
        AutorisationSoutenanceDTO dto = new AutorisationSoutenanceDTO();
        dto.setId(entity.getId());

        if (entity.getDefenseRequest() != null) {
            dto.setDefenseRequestId(entity.getDefenseRequest().getId());
        }

        dto.setStatut(entity.getStatut());
        dto.setAdministrateurId(entity.getAdministrateurId());
        dto.setDateAutorisation(entity.getDateAutorisation());

        // Verification results
        dto.setPrerequisValides(entity.getPrerequisValides());
        dto.setJuryComplet(entity.getJuryComplet());
        dto.setRapportsFavorables(entity.getRapportsFavorables());
        dto.setDocumentsComplets(entity.getDocumentsComplets());

        dto.setCommentaireAdmin(entity.getCommentaireAdmin());
        dto.setMotifRefus(entity.getMotifRefus());

        // Scheduling info
        dto.setDateSoutenance(entity.getDateSoutenance());
        dto.setLieuSoutenance(entity.getLieuSoutenance());
        dto.setSalleSoutenance(entity.getSalleSoutenance());

        // Enrich with administrator information from user-service (with Resilience4j)
        if (entity.getAdministrateurId() != null) {
            UserDTO admin = userServiceFacade.getUserById(entity.getAdministrateurId());
            dto.setAdministrateurFirstName(admin.getFirstName());
            dto.setAdministrateurLastName(admin.getLastName());
            dto.setAdministrateurEmail(admin.getEmail());
        }

        return dto;
    }

    /**
     * Update entity with verification results
     */
    public void updateVerificationResults(AutorisationSoutenance entity,
            boolean prerequisValides,
            boolean juryComplet,
            boolean rapportsFavorables,
            boolean documentsComplets) {
        entity.setPrerequisValides(prerequisValides);
        entity.setJuryComplet(juryComplet);
        entity.setRapportsFavorables(rapportsFavorables);
        entity.setDocumentsComplets(documentsComplets);
    }
}
