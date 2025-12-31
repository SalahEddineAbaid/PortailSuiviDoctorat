package ma.emsi.inscriptionservice.entities;

import jakarta.persistence.*;
import lombok.*;
import ma.emsi.inscriptionservice.enums.StatutDerogation;
import java.time.LocalDateTime;

@Entity
@Table(name = "derogation_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DerogationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Column(nullable = false, length = 2000)
    private String motif;

    @Column(nullable = false)
    private LocalDateTime dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDerogation statut;

    private Long validateurId;

    @Column(length = 1000)
    private String commentaireValidation;

    private LocalDateTime dateValidation;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] documentsJustificatifs;

    @PrePersist
    protected void onCreate() {
        dateDemande = LocalDateTime.now();
        statut = StatutDerogation.EN_ATTENTE;
    }
}
