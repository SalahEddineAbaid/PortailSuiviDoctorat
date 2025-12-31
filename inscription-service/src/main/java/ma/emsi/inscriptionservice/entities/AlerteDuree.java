package ma.emsi.inscriptionservice.entities;

import jakarta.persistence.*;
import lombok.*;
import ma.emsi.inscriptionservice.enums.TypeAlerte;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertes_duree")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteDuree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAlerte type;

    @Column(nullable = false)
    private LocalDateTime dateAlerte;

    @Column(nullable = false)
    private boolean traite;

    @Column(length = 500)
    private String action;

    @PrePersist
    protected void onCreate() {
        dateAlerte = LocalDateTime.now();
        traite = false;
    }
}
