package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.client.UserServiceClient;
import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.enums.MemberStatus;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.repository.JuryMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JuryMemberService {

    @Autowired
    private JuryMemberRepository juryMemberRepository;

    @Autowired
    private UserServiceClient userServiceClient;

    public JuryMember add(JuryMember member) {
        // ✅ IMPORTANT 3 : Validation du professeur
        try {
            UserDTO professor = userServiceClient.getUserById(member.getProfessorId());

            // Vérifier que l'utilisateur a le rôle PROFESSEUR ou DIRECTEUR
            if (professor.getRoles() == null ||
                    (!professor.getRoles().contains("ROLE_PROFESSEUR") &&
                            !professor.getRoles().contains("ROLE_DIRECTEUR"))) {
                throw new IllegalArgumentException(
                        "L'utilisateur avec l'ID " + member.getProfessorId() +
                                " n'a pas le rôle PROFESSEUR ou DIRECTEUR. Rôles: " + professor.getRoles());
            }
        } catch (feign.FeignException e) {
            throw new ResourceNotFoundException(
                    "Professeur avec l'ID " + member.getProfessorId() +
                            " introuvable dans user-service. Erreur: " + e.status());
        }

        return juryMemberRepository.save(member);
    }

    public List<JuryMember> getByJury(Long juryId) {
        return juryMemberRepository.findByJuryId(juryId);
    }

    public JuryMember updateStatus(Long id, MemberStatus status) {
        JuryMember member = juryMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JuryMember not found"));

        member.setStatus(status);
        return juryMemberRepository.save(member);
    }
}
