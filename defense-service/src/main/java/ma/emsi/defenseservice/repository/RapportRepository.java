package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.Rapport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, Long> {
    List<Rapport> findByDefenseRequestId(Long defenseRequestId);

    List<Rapport> findByJuryMemberId(Long juryMemberId);
}
