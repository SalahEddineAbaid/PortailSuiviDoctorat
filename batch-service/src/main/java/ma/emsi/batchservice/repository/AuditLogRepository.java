package ma.emsi.batchservice.repository;

import ma.emsi.batchservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Audit Log
 * Note: Delete operations are intentionally restricted to prevent audit log
 * deletion
 * Requirements: 10.9
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by event type
     */
    List<AuditLog> findByEventTypeOrderByTimestampDesc(String eventType);

    /**
     * Find audit logs by user
     */
    List<AuditLog> findByUserIdOrderByTimestampDesc(String userId);

    /**
     * Find audit logs by job name
     */
    List<AuditLog> findByJobNameOrderByTimestampDesc(String jobName);

    /**
     * Find audit logs within a time range
     */
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Find recent audit logs
     */
    List<AuditLog> findTop100ByOrderByTimestampDesc();

    /**
     * Override delete methods to prevent audit log deletion
     * These methods will throw UnsupportedOperationException
     */
    @Override
    @Modifying
    @Query("SELECT 1 FROM AuditLog WHERE 1=0")
    default void deleteById(Long id) {
        throw new UnsupportedOperationException("Audit logs cannot be deleted - they must be retained indefinitely");
    }

    @Override
    default void delete(AuditLog entity) {
        throw new UnsupportedOperationException("Audit logs cannot be deleted - they must be retained indefinitely");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Audit logs cannot be deleted - they must be retained indefinitely");
    }

    @Override
    default void deleteAll(Iterable<? extends AuditLog> entities) {
        throw new UnsupportedOperationException("Audit logs cannot be deleted - they must be retained indefinitely");
    }

    @Override
    default void deleteAllById(Iterable<? extends Long> ids) {
        throw new UnsupportedOperationException("Audit logs cannot be deleted - they must be retained indefinitely");
    }
}
