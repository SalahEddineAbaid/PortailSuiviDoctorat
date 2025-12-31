package ma.emsi.inscriptionservice.entities;

import jakarta.persistence.*;
import lombok.*;
import ma.emsi.inscriptionservice.enums.TypeDocumentGenere;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents_generes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentGenere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocumentGenere type;

    @Column(nullable = false, length = 500)
    private String cheminFichier;

    @Column(nullable = false)
    private LocalDateTime dateGeneration;

    @Column(nullable = false)
    private Long tailleFichier;

    @PrePersist
    protected void onCreate() {
        dateGeneration = LocalDateTime.now();
    }
}
