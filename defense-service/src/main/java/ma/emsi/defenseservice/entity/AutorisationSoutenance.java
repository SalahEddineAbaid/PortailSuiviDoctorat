package ma.emsi.defenseservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ma.emsi.defenseservice.enums.StatutAutorisation;

import java.time.LocalDateTime;

@Entity
@Table(name = "autorisations_soutenance", indexes = {
        @Index(name = "idx_defense_request_id", columnList = "defense_request_id"),
        @Index(name = "idx_statut", columnList = "statut")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutorisationSoutenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "defense_request_id", unique = true)
    private DefenseRequest defenseRequest;

    @Enumerated(EnumType.STRING)
    private StatutAutorisation statut;

    private Long administrateurId;
    private LocalDateTime dateAutorisation;

    // Verification results
    private Boolean prerequisValides;
    private Boolean juryComplet;
    private Boolean rapportsFavorables;
    private Boolean documentsComplets;

    @Column(columnDefinition = "TEXT")
    private String commentaireAdmin;

    @Column(columnDefinition = "TEXT")
    private String motifRefus;

    // Scheduling info
    private LocalDateTime dateSoutenance;
    private String lieuSoutenance;
    private String salleSoutenance;
}
