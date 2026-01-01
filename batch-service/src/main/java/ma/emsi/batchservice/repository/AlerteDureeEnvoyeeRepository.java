package ma.emsi.batchservice.repository;

import ma.emsi.batchservice.entity.AlerteDureeEnvoyee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for AlerteDureeEnvoyee entity.
 * Provides access to sent duration alert records for idempotence checking.
 * 
 * Requirements: 2.7
 */
@Repository
public interface AlerteDureeEnvoyeeRepository extends JpaRepository<AlerteDureeEnvoyee, Long> {

    /**
     * Check if an alert has already been sent for a specific enrollment and alert
     * type.
     * Used to prevent duplicate notifications.
     * 
     * @param inscriptionId The enrollment ID
     * @param typeAlerte    The alert type (3_ANS, 6_ANS, DEPASSEMENT)
     * @return Optional containing the alert record if it exists
     */
    Optional<AlerteDureeEnvoyee> findByInscriptionIdAndTypeAlerte(Long inscriptionId, String typeAlerte);

    /**
     * Check if an alert exists for a specific enrollment and alert type.
     * 
     * @param inscriptionId The enrollment ID
     * @param typeAlerte    The alert type
     * @return true if alert has been sent, false otherwise
     */
    boolean existsByInscriptionIdAndTypeAlerte(Long inscriptionId, String typeAlerte);
}
