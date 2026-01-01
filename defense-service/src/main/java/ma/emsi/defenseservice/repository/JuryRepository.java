package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.Jury;
import ma.emsi.defenseservice.enums.JuryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JuryRepository extends JpaRepository<Jury, Long> {
    Optional<Jury> findByDefenseRequestId(Long defenseRequestId);

    List<Jury> findByDirectorId(Long directorId);

    @Query("SELECT COUNT(j) FROM Jury j WHERE j.directorId = :directorId AND j.status = :status")
    long countByDirectorIdAndStatus(@Param("directorId") Long directorId, @Param("status") JuryStatus status);
}
