package ma.emsi.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import ma.emsi.userservice.enums.AuditAction;

import java.time.LocalDateTime;

/**
 * Entity representing audit records for security-sensitive user actions.
 * Tracks all important operations performed by users for security monitoring
 * and investigation.
 */
@Entity
@Table(name = "user_audits", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_action", columnList = "action"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;
}
