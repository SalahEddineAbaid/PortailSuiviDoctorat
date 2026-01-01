package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.Defense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DefenseRepository extends JpaRepository<Defense, Long> {
    Optional<Defense> findByDefenseRequestId(Long defenseRequestId);

    @Query("SELECT d FROM Defense d " +
            "WHERE d.defenseRequest.jury.directorId = :directorId " +
            "ORDER BY d.defenseDate ASC")
    List<Defense> findScheduledDefensesByDirector(@Param("directorId") Long directorId);
}
