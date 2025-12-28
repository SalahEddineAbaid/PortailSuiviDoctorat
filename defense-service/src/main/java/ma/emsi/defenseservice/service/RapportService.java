package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.entity.Rapport;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.repository.JuryMemberRepository;
import ma.emsi.defenseservice.repository.RapportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RapportService {

    @Autowired
    private RapportRepository rapportRepository;

    @Autowired
    private JuryMemberRepository juryMemberRepository;

    @Autowired
    private UserServiceFacade userServiceFacade;

    public Rapport submit(Rapport r) {
        // ✅ IMPORTANT 4 : Validation de l'evaluateur (membre du jury) (avec
        // Resilience4j)
        if (r.getJuryMember() != null && r.getJuryMember().getId() != null) {
            JuryMember juryMember = juryMemberRepository.findById(r.getJuryMember().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Membre du jury avec l'ID " + r.getJuryMember().getId() + " introuvable"));

            // Vérifier que le professeur existe et a le bon rôle
            boolean isValidProfessor = userServiceFacade.validateUserRole(
                    juryMember.getProfessorId(),
                    "ROLE_PROFESSEUR")
                    || userServiceFacade.validateUserRole(
                            juryMember.getProfessorId(),
                            "ROLE_DIRECTEUR");

            if (!isValidProfessor) {
                throw new IllegalArgumentException(
                        "L'évaluateur avec l'ID " + juryMember.getProfessorId() +
                                " n'existe pas ou n'a pas le rôle PROFESSEUR ou DIRECTEUR");
            }
        }

        r.setSubmissionDate(LocalDateTime.now());
        return rapportRepository.save(r);
    }

    public List<Rapport> getByDefenseRequest(Long defenseRequestId) {
        return rapportRepository.findByDefenseRequestId(defenseRequestId);
    }
}
