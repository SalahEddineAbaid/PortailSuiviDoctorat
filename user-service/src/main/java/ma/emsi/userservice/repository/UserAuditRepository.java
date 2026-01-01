package ma.emsi.userservice.repository;

import ma.emsi.userservice.entity.UserAudit;
import ma.emsi.userservice.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for UserAudit entity.
 * Provides methods for querying audit records with pagination and filtering.
 */
public interface UserAuditRepository extends JpaRepository<UserAudit, Long> {

    /**
     * Find all audit records for a specific user, ordered by timestamp descending.
     *
     * @param userId   the user ID to filter by
     * @param pageable pagination information
     * @return paginated audit records
     */
    Page<UserAudit> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    /**
     * Find audit records for a specific user and action type, ordered by timestamp
     * descending.
     *
     * @param userId   the user ID to filter by
     * @param action   the audit action type to filter by
     * @param pageable pagination information
     * @return paginated audit records
     */
    Page<UserAudit> findByUserIdAndActionOrderByTimestampDesc(
            Long userId, AuditAction action, Pageable pageable);

    /**
     * Find all audit records after a specific timestamp.
     *
     * @param timestamp the timestamp to filter from
     * @return list of audit records
     */
    List<UserAudit> findByTimestampAfter(LocalDateTime timestamp);

    /**
     * Count audit records by action type within a date range.
     *
     * @param action the audit action type
     * @param start  start of the date range
     * @param end    end of the date range
     * @return count of matching audit records
     */
    long countByActionAndTimestampBetween(
            AuditAction action, LocalDateTime start, LocalDateTime end);

    /**
     * Find audit records by action type within a date range.
     *
     * @param action the audit action type
     * @param start  start of the date range
     * @param end    end of the date range
     * @return list of matching audit records
     */
    List<UserAudit> findByActionAndTimestampBetween(
            AuditAction action, LocalDateTime start, LocalDateTime end);
}
