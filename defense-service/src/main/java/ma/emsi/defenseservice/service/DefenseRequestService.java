package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.entity.Prerequisites;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.repository.DefenseRequestRepository;
import ma.emsi.defenseservice.repository.PrerequisitesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DefenseRequestService {

    @Autowired
    private DefenseRequestRepository defenseRequestRepository;

    @Autowired
    private PrerequisitesRepository prerequisitesRepository;

    @Autowired
    private UserServiceFacade userServiceFacade;

    public DefenseRequest create(DefenseRequest request, Long prerequisitesId) {
        // ✅ VALIDATION : Vérifier que le doctorant existe et a le rôle DOCTORANT (avec
        // Resilience4j)
        boolean isValidDoctorant = userServiceFacade.validateUserRole(
                request.getDoctorantId(),
                "ROLE_DOCTORANT");

        if (!isValidDoctorant) {
            throw new IllegalArgumentException(
                    "L'utilisateur avec l'ID " + request.getDoctorantId() +
                            " n'existe pas ou n'a pas le rôle DOCTORANT");
        }

        request.setSubmissionDate(LocalDateTime.now());

        // Définir le statut par défaut si non fourni
        if (request.getStatus() == null) {
            request.setStatus(DefenseRequestStatus.SUBMITTED);
        }

        // ✅ CRITIQUE 2 : Validation des Prerequisites
        if (prerequisitesId != null) {
            Prerequisites prerequisites = prerequisitesRepository.findById(prerequisitesId)
                    .orElseThrow(() -> new ResourceNotFoundException("Prerequisites not found"));

            // ✅ Vérifier que les prérequis sont validés
            if (!prerequisites.isValid()) {
                throw new IllegalStateException(
                        "Les prérequis doivent être validés par le directeur avant de créer une demande de soutenance");
            }

            // ✅ Vérifier que les prérequis appartiennent au doctorant
            if (!prerequisites.getDoctorantId().equals(request.getDoctorantId())) {
                throw new IllegalArgumentException(
                        "Les prérequis (ID: " + prerequisitesId + ") n'appartiennent pas au doctorant (ID: " +
                                request.getDoctorantId() + ")");
            }

            request.setPrerequisites(prerequisites);
        }

        return defenseRequestRepository.save(request);
    }

    public DefenseRequest getById(Long id) {
        return defenseRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DefenseRequest not found"));
    }

    public List<DefenseRequest> getAll() {
        return defenseRequestRepository.findAll();
    }

    public DefenseRequest updateStatus(Long id, DefenseRequestStatus status) {
        DefenseRequest req = getById(id);
        req.setStatus(status);
        return defenseRequestRepository.save(req);
    }

    public void delete(Long id) {
        defenseRequestRepository.deleteById(id);
    }
}
