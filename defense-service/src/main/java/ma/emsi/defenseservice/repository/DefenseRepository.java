package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.Defense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DefenseRepository extends JpaRepository<Defense,Long> {
    Optional<Defense> findByDefenseRequestId(Long defenseRequestId);
}
