package ma.emsi.defenseservice.repository;

import ma.emsi.defenseservice.entity.Publication;
import ma.emsi.defenseservice.enums.QuartileJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PublicationRepository extends JpaRepository<Publication, Long> {

    /**
     * Find all publications for a given prerequisites
     */
    List<Publication> findByPrerequisitesId(Long prerequisitesId);

    /**
     * Find all publications pending validation (valide = false)
     */
    List<Publication> findByValide(boolean valide);

    /**
     * Count valid Q1/Q2 publications for a given prerequisites
     */
    @Query("SELECT COUNT(p) FROM Publication p WHERE p.prerequisites.id = :prerequisitesId " +
            "AND p.valide = true AND (p.quartile = :q1 OR p.quartile = :q2)")
    long countValidQ1Q2Publications(@Param("prerequisitesId") Long prerequisitesId,
            @Param("q1") QuartileJournal q1,
            @Param("q2") QuartileJournal q2);

    /**
     * Find all valid Q1/Q2 publications for a given prerequisites
     */
    @Query("SELECT p FROM Publication p WHERE p.prerequisites.id = :prerequisitesId " +
            "AND p.valide = true AND (p.quartile = :q1 OR p.quartile = :q2)")
    List<Publication> findValidQ1Q2Publications(@Param("prerequisitesId") Long prerequisitesId,
            @Param("q1") QuartileJournal q1,
            @Param("q2") QuartileJournal q2);
}
