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

    public JuryMember add(JuryMember member) {
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

