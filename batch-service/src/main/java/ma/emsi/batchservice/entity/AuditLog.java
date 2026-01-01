package ma.emsi.batchservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit Log Entity
 * Stores immutable audit records for all batch operations
 * Requirements: 10.6, 10.7, 10.8, 10.9
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_event_type", columnList = "eventType"),
        @Index(name = "idx_audit_user", columnList = "userId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 50)
    private String eventType; // JOB_EXECUTION, ARCHIVE_ACCESS, MANUAL_TRIGGER

    @Column(nullable = false, length = 100)
    private String eventAction; // START, COMPLETE, FAIL, ACCESS, TRIGGER

    @Column(length = 100)
    private String jobName;

    @Column
    private Long executionId;

    @Column(length = 100)
    private String userId; // Email or user identifier

    @Column(length = 50)
    private String userRole;

    @Column(length = 500)
    private String resourcePath; // For archive access

    @Column(length = 1000)
    private String details;

    @Column(length = 50)
    private String status; // SUCCESS, FAILURE

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 200)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
