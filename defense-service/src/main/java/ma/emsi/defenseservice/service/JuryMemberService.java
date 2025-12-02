package ma.emsi.defenseservice.service;

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
    private UserServiceFacade userServiceFacade;

    public JuryMember add(JuryMember member) {
        // ✅ IMPORTANT 3 : Validation du professeur (avec Resilience4j)
        boolean isValidProfessor = userServiceFacade.validateUserRole(
                member.getProfessorId(),
                "ROLE_PROFESSEUR")
                || userServiceFacade.validateUserRole(
                        member.getProfessorId(),
                        "ROLE_DIRECTEUR");

        if (!isValidProfessor) {
            throw new IllegalArgumentException(
                    "L'utilisateur avec l'ID " + member.getProfessorId() +
                            " n'existe pas ou n'a pas le rôle PROFESSEUR ou DIRECTEUR");
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
