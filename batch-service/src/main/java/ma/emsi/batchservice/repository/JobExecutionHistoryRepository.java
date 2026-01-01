package ma.emsi.batchservice.repository;

import ma.emsi.batchservice.entity.JobExecutionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for JobExecutionHistory entity.
 * Provides access to job execution history records.
 */
@Repository
public interface JobExecutionHistoryRepository extends JpaRepository<JobExecutionHistory, Long> {

    /**
     * Find all executions for a specific job, ordered by start time descending.
     */
    List<JobExecutionHistory> findByJobNameOrderByStartTimeDesc(String jobName);

    /**
     * Find executions within a date range.
     */
    List<JobExecutionHistory> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Find failed executions.
     */
    List<JobExecutionHistory> findByStatus(String status);
}
