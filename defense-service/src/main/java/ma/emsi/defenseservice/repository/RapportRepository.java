package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.Rapport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RapportRepository extends JpaRepository<Rapport, Long> {
    List<Rapport> findByDefenseRequestId(Long defenseRequestId);

    List<Rapport> findByJuryMemberId(Long juryMemberId);

    @Query("SELECT COUNT(r) FROM Rapport r " +
            "WHERE r.defenseRequest.jury.directorId = :directorId " +
            "AND r.submissionDate IS NULL")
    long countPendingReportsByDirector(@Param("directorId") Long directorId);
}
