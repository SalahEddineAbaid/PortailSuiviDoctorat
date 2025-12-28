package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.entity.Prerequisites;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.repository.PrerequisitesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrerequisitesService {

    @Autowired
    private PrerequisitesRepository prerequisitesRepository;

    @Autowired
    private UserServiceFacade userServiceFacade;

    public Prerequisites save(Prerequisites p) {
        // ✅ IMPORTANT 2 : Validation du doctorant (avec Resilience4j)
        boolean isValidDoctorant = userServiceFacade.validateUserRole(
                p.getDoctorantId(),
                "ROLE_DOCTORANT");

        if (!isValidDoctorant) {
            throw new IllegalArgumentException(
                    "L'utilisateur avec l'ID " + p.getDoctorantId() +
                            " n'existe pas ou n'a pas le rôle DOCTORANT");
        }

        return prerequisitesRepository.save(p);
    }

    public Prerequisites getById(Long id) {
        return prerequisitesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prerequisites not found"));
    }

    public Prerequisites validate(Long id, boolean isValid) {
        Prerequisites pre = getById(id);
        pre.setValid(isValid);
        return prerequisitesRepository.save(pre);
    }

    /**
     * ✅ AMÉLIORATION : Récupérer tous les prérequis d'un doctorant
     */
    public List<Prerequisites> getByDoctorant(Long doctorantId) {
        return prerequisitesRepository.findByDoctorantId(doctorantId);
    }

    /**
     * ✅ AMÉLIORATION : Récupérer les prérequis validés d'un doctorant
     */
    public List<Prerequisites> getValidatedByDoctorant(Long doctorantId) {
        return prerequisitesRepository.findByDoctorantIdAndIsValid(doctorantId, true);
    }
}
