package ma.emsi.defenseservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.emsi.defenseservice.enums.QuartileJournal;
import ma.emsi.defenseservice.enums.TypePublication;

import java.time.LocalDateTime;

@Entity
@Table(name = "publications", indexes = {
        @Index(name = "idx_prerequisites_id", columnList = "prerequisites_id"),
        @Index(name = "idx_valide", columnList = "valide")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisites_id", nullable = false)
    private Prerequisites prerequisites;

    @Column(length = 500, nullable = false)
    private String titre;

    @Column(length = 255)
    private String journal;

    @Column(name = "annee_publication")
    private Integer anneePublication;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private TypePublication type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QuartileJournal quartile;

    @Column(length = 255)
    private String doi;

    @Column(length = 500)
    private String url;

    // Validation fields
    @Column(nullable = false)
    private boolean valide = false;

    @Column(name = "validateur_id")
    private Long validateurId;

    @Column(name = "commentaire_validation", columnDefinition = "TEXT")
    private String commentaireValidation;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;
}
