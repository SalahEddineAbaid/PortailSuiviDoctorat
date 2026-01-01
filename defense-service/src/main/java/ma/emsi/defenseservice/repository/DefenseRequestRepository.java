package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DefenseRequestRepository extends JpaRepository<DefenseRequest, Long> {

    @Query("SELECT dr FROM DefenseRequest dr WHERE dr.prerequisites.doctorantId IN " +
            "(SELECT p.doctorantId FROM Prerequisites p WHERE p.id IN " +
            "(SELECT j.defenseRequest.prerequisites.id FROM Jury j WHERE j.directorId = :directorId))")
    List<DefenseRequest> findByDirectorId(@Param("directorId") Long directorId);

    @Query("SELECT COUNT(DISTINCT dr.prerequisites.doctorantId) FROM DefenseRequest dr " +
            "WHERE dr.prerequisites.doctorantId IN " +
            "(SELECT p.doctorantId FROM Prerequisites p WHERE p.id IN " +
            "(SELECT j.defenseRequest.prerequisites.id FROM Jury j WHERE j.directorId = :directorId)) " +
            "AND dr.status NOT IN (:excludedStatuses)")
    long countActiveDoctorantsByDirector(@Param("directorId") Long directorId,
            @Param("excludedStatuses") List<DefenseRequestStatus> excludedStatuses);

    @Query("SELECT COUNT(dr) FROM DefenseRequest dr " +
            "WHERE dr.jury.directorId = :directorId " +
            "AND dr.status IN (:statuses)")
    long countByDirectorIdAndStatusIn(@Param("directorId") Long directorId,
            @Param("statuses") List<DefenseRequestStatus> statuses);
}
