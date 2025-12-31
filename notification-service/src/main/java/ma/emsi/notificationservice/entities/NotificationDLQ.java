package ma.emsi.notificationservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.notificationservice.enums.TypeNotification;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a notification in the Dead Letter Queue.
 * Used for audit and tracking of failed notifications that require manual investigation.
 * 
 * Requirements: 11.2, 11.3
 */
@Entity
@Table(name = "notification_dlq")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDLQ {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Reference to the original notification ID that failed.
     */
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;
    
    /**
     * Type of the notification that failed.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TypeNotification type;
    
    /**
     * Email address of the intended recipient.
     */
    @Column(nullable = false, length = 255)
    private String destinataire;
    
    /**
     * Subject of the notification.
     */
    @Column(length = 255)
    private String sujet;
    
    /**
     * Error message that caused the notification to fail.
     */
    @Column(name = "erreur_message", columnDefinition = "TEXT")
    private String erreurMessage;
    
    /**
     * Full notification data as JSON for reprocessing.
     */
    @Column(columnDefinition = "JSON")
    private String donnees;
    
    /**
     * Number of times this notification was attempted before DLQ.
     */
    @Column(name = "nombre_tentatives")
    private Integer nombreTentatives;
    
    /**
     * Timestamp when the notification was added to DLQ.
     */
    @Column(name = "date_ajout_dlq", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime dateAjoutDlq;
    
    /**
     * Timestamp of the last reprocessing attempt.
     */
    @Column(name = "date_derniere_tentative")
    private LocalDateTime dateDerniereTentative;
    
    /**
     * Whether this DLQ entry has been successfully reprocessed.
     */
    @Column(name = "retraite")
    @Builder.Default
    private Boolean retraite = false;
    
    /**
     * Kafka partition from which the message was consumed.
     */
    @Column(name = "kafka_partition")
    private Integer kafkaPartition;
    
    /**
     * Kafka offset of the original message.
     */
    @Column(name = "kafka_offset")
    private Long kafkaOffset;
}
