package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.entity.Jury;
import ma.emsi.defenseservice.enums.JuryStatus;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.repository.JuryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class JuryService {

    @Autowired
    private JuryRepository juryRepository;

    @Autowired
    private UserServiceFacade userServiceFacade;

    public Jury create(Jury jury) {
        // ✅ IMPORTANT 1 : Validation du directeur (avec Resilience4j)
        boolean isValidDirector = userServiceFacade.validateUserRole(
                jury.getDirectorId(),
                "ROLE_DIRECTEUR");

        if (!isValidDirector) {
            throw new IllegalArgumentException(
                    "L'utilisateur avec l'ID " + jury.getDirectorId() +
                            " n'existe pas ou n'a pas le rôle DIRECTEUR");
        }

        jury.setProposalDate(LocalDateTime.now());
        return juryRepository.save(jury);
    }

    public Jury getByDefenseRequest(Long defenseRequestId) {
        return juryRepository.findByDefenseRequestId(defenseRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Jury not found"));
    }

    public Jury updateStatus(Long id, JuryStatus status) {
        Jury jury = juryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Jury not found"));

        jury.setStatus(status);

        if (status == JuryStatus.VALIDATED_BY_ADMIN) {
            jury.setValidationDate(LocalDateTime.now());
        }

        return juryRepository.save(jury);
    }
}
