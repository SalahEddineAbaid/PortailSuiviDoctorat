package ma.emsi.batchservice.repository;

import ma.emsi.batchservice.entity.InscriptionArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for InscriptionArchive entity.
 * 
 * Requirements: 4.3
 */
@Repository
public interface InscriptionArchiveRepository extends JpaRepository<InscriptionArchive, Long> {
}
