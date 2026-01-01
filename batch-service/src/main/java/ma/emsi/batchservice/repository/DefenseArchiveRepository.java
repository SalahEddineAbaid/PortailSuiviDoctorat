package ma.emsi.batchservice.repository;

import ma.emsi.batchservice.entity.DefenseArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for DefenseArchive entity.
 * 
 * Requirements: 4.5
 */
@Repository
public interface DefenseArchiveRepository extends JpaRepository<DefenseArchive, Long> {
}
