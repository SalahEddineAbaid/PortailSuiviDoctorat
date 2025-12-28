package ma.emsi.inscriptionservice.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "infos_these")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfosThese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Column(nullable = false, length = 500)
    private String titreThese;

    @Column(nullable = false)
    private String discipline;

    @Column(nullable = false)
    private String laboratoire;

    @Column(nullable = false)
    private String etablissementAccueil;

    private Boolean cotutelle = false;

    private String universitePartenaire;

    private String paysPartenaire;

    @Column(nullable = false)
    private LocalDate dateDebutPrevue;
}
