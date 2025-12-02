package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.client.UserServiceClient;
import ma.emsi.defenseservice.dto.external.UserDTO;
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
    private UserServiceClient userServiceClient;

    public Rapport submit(Rapport r) {
        // ✅ IMPORTANT 4 : Validation de l'evaluateur (membre du jury)
        if (r.getJuryMember() != null && r.getJuryMember().getId() != null) {
            JuryMember juryMember = juryMemberRepository.findById(r.getJuryMember().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Membre du jury avec l'ID " + r.getJuryMember().getId() + " introuvable"));

            // Vérifier que le professeur existe dans user-service
            try {
                UserDTO professor = userServiceClient.getUserById(juryMember.getProfessorId());

                // Vérifier que l'utilisateur a le rôle PROFESSEUR ou DIRECTEUR
                if (professor.getRoles() == null ||
                        (!professor.getRoles().contains("ROLE_PROFESSEUR") &&
                                !professor.getRoles().contains("ROLE_DIRECTEUR"))) {
                    throw new IllegalArgumentException(
                            "L'évaluateur avec l'ID " + juryMember.getProfessorId() +
                                    " n'a pas le rôle PROFESSEUR ou DIRECTEUR. Rôles: " + professor.getRoles());
                }
            } catch (feign.FeignException e) {
                throw new ResourceNotFoundException(
                        "Évaluateur avec l'ID " + juryMember.getProfessorId() +
                                " introuvable dans user-service. Erreur: " + e.status());
            }
        }

        r.setSubmissionDate(LocalDateTime.now());
        return rapportRepository.save(r);
    }

    public List<Rapport> getByDefenseRequest(Long defenseRequestId) {
        return rapportRepository.findByDefenseRequestId(defenseRequestId);
    }
}
