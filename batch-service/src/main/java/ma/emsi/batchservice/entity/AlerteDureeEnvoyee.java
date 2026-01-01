package ma.emsi.batchservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing sent duration alerts to prevent duplicate notifications.
 * Ensures idempotence by tracking which alerts have been sent for each
 * enrollment.
 */
@Entity
@Table(name = "alerte_duree_envoyee", uniqueConstraints = {
        @UniqueConstraint(name = "uk_inscription_type", columnNames = { "inscription_id", "type_alerte" })
}, indexes = {
        @Index(name = "idx_inscription_id", columnList = "inscription_id"),
        @Index(name = "idx_doctorant_id", columnList = "doctorant_id"),
        @Index(name = "idx_type_alerte", columnList = "type_alerte"),
        @Index(name = "idx_date_envoi", columnList = "date_envoi")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteDureeEnvoyee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inscription_id", nullable = false)
    private Long inscriptionId;

    @Column(name = "doctorant_id", nullable = false)
    private Long doctorantId;

    @Column(name = "type_alerte", nullable = false, length = 50)
    private String typeAlerte; // 3_ANS, 6_ANS, DEPASSEMENT

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    @PrePersist
    protected void onCreate() {
        if (dateEnvoi == null) {
            dateEnvoi = LocalDateTime.now();
        }
    }
}
