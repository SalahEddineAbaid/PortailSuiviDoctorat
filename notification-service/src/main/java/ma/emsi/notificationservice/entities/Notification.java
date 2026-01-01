package ma.emsi.notificationservice.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a notification in the system.
 * Stores both the text and HTML versions of the message along with metadata.
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Notification entity representing an email notification in the system")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the notification", example = "1")
    private Long id;

    @Column(name = "user_id")
    @Schema(description = "User ID of the notification recipient", example = "7")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Schema(description = "Type of notification", example = "INSCRIPTION_VALIDEE_ADMIN")
    private TypeNotification type;

    @Column(nullable = false, length = 255)
    @Schema(description = "Recipient email address", example = "doctorant@emsi.ma")
    private String destinataire;

    @Column(length = 255)
    @Schema(description = "Email subject line", example = "Votre inscription a été validée")
    private String sujet;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Plain text version of the message")
    private String messageTexte;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "HTML version of the message")
    private String messageHtml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Schema(description = "Current status of the notification", example = "SENT")
    private StatutNotification statut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    @Schema(description = "Priority level of the notification", example = "NORMALE")
    private PrioriteNotification priorite = PrioriteNotification.NORMALE;

    @Column(columnDefinition = "JSON")
    @Schema(description = "JSON data used for template variable interpolation")
    private String donnees;

    @Column(name = "nombre_tentatives")
    @Builder.Default
    @Schema(description = "Number of send attempts", example = "0")
    private Integer nombreTentatives = 0;

    @Column(name = "erreur_message", columnDefinition = "TEXT")
    @Schema(description = "Error message if the notification failed")
    private String erreurMessage;

    @Column(name = "lu")
    @Builder.Default
    @Schema(description = "Whether the notification has been read", example = "false")
    private Boolean lu = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @CreationTimestamp
    @Schema(description = "Timestamp when the notification was created", example = "2024-01-15T10:30:00")
    private LocalDateTime dateCreation;

    @Column(name = "date_envoi")
    @Schema(description = "Timestamp when the notification was successfully sent", example = "2024-01-15T10:30:15")
    private LocalDateTime dateEnvoi;

    @Column(name = "date_modification")
    @UpdateTimestamp
    @Schema(description = "Timestamp of the last modification", example = "2024-01-15T10:30:15")
    private LocalDateTime dateModification;
}
