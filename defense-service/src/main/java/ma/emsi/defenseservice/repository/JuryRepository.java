package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.Jury;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JuryRepository extends JpaRepository<Jury, Long> {
    Optional<Jury> findByDefenseRequestId(Long defenseRequestId);
}
