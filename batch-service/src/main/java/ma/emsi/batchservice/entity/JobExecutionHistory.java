package ma.emsi.batchservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing the execution history of batch jobs.
 * Stores detailed information about each job execution for monitoring and
 * auditing.
 */
@Entity
@Table(name = "job_execution_history", indexes = {
        @Index(name = "idx_job_name", columnList = "job_name"),
        @Index(name = "idx_start_time", columnList = "start_time"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // COMPLETED, FAILED, STOPPED

    @Column(name = "exit_message", columnDefinition = "TEXT")
    private String exitMessage;

    @Column(name = "items_processed")
    private Integer itemsProcessed;

    @Column(name = "items_failed")
    private Integer itemsFailed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
