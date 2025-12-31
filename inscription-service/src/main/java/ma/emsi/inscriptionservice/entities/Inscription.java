package ma.emsi.inscriptionservice.entities;

import jakarta.persistence.*;
import lombok.*;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.enums.TypeInscription;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence au User-Service
    @Column(nullable = false)
    private Long doctorantId;

    @Column(nullable = false)
    private Long directeurTheseId;

    @ManyToOne
    @JoinColumn(name = "campagne_id", nullable = false)
    private Campagne campagne;

    @Column(nullable = false, length = 500)
    private String sujetThese;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeInscription type;

    @Column(nullable = false)
    private Integer anneeInscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutInscription statut = StatutInscription.BROUILLON;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime dateValidation;

    private LocalDateTime datePremiereInscription;

    @Column(length = 1000)
    private String commentaireDirecteur;

    @Column(length = 1000)
    private String commentaireAdmin;

    @Builder.Default
    private Boolean derogation = false;

    @Column(length = 500)
    private String motifDerogation;

    @OneToOne(mappedBy = "inscription", cascade = CascadeType.ALL)
    private InfosDoctorant infosDoctorant;

    @OneToOne(mappedBy = "inscription", cascade = CascadeType.ALL)
    private InfosThese infosThese;

    @OneToMany(mappedBy = "inscription", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DocumentInscription> documents = new ArrayList<>();

    @OneToMany(mappedBy = "inscription", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ValidationInscription> validations = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean bloqueReInscription = false;

    @OneToMany(mappedBy = "inscription", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DerogationRequest> derogations = new ArrayList<>();

    @OneToMany(mappedBy = "inscription", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AlerteDuree> alertes = new ArrayList<>();

    @OneToMany(mappedBy = "inscription", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DocumentGenere> documentsGeneres = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
    }

    public boolean verifierDelais() {
        if (datePremiereInscription == null) {
            return true;
        }
        long duree = ChronoUnit.YEARS.between(datePremiereInscription, LocalDateTime.now());
        return duree <= 3 || derogation;
    }

    public long calculerDuree() {
        if (datePremiereInscription == null) {
            return 0;
        }
        return ChronoUnit.YEARS.between(datePremiereInscription, LocalDateTime.now());
    }

    public boolean depasseLimiteDuree() {
        return calculerDuree() >= 6;
    }
}
