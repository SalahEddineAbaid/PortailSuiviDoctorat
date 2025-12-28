package ma.emsi.inscriptionservice.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.TypeCampagne;

import java.time.LocalDate;

@Entity
@Table(name = "campagnes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campagne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCampagne type;

    @Column(nullable = false)
    private LocalDate dateDebut;

    @Column(nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false)
    private Boolean active = true;

    private Integer anneeUniversitaire;

    public boolean isOuverte() {
        LocalDate now = LocalDate.now();
        return active && !now.isBefore(dateDebut) && !now.isAfter(dateFin);
    }

    public void fermer() {
        this.active = false;
    }
}
