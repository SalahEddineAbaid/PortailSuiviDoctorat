package ma.emsi.inscriptionservice.entities;

import jakarta.persistence.*;
import lombok.*;
import ma.emsi.inscriptionservice.enums.StatutValidation;
import ma.emsi.inscriptionservice.enums.TypeValidateur;
import java.time.LocalDateTime;

@Entity
@Table(name = "validations_inscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationInscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Column(nullable = false)
    private Long validateurId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeValidateur typeValidateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutValidation statut = StatutValidation.EN_ATTENTE;

    @Column(length = 1000)
    private String commentaire;

    private LocalDateTime dateValidation;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }
}
