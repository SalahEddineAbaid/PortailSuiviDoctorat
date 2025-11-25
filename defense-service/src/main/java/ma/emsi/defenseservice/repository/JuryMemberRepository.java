package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JuryMemberRepository extends JpaRepository<JuryMember,Long> {
    List<JuryMember> findByJuryId(Long juryId);

    List<JuryMember> findByStatus(MemberStatus status);
}
