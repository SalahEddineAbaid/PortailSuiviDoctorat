package ma.emsi.inscriptionservice.entities;

import jakarta.persistence.*;
import lombok.*;
import ma.emsi.inscriptionservice.enums.TypeDocument;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents_inscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentInscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocument typeDocument;

    @Column(nullable = false)
    private String nomFichier;

    @Column(nullable = false)
    private String cheminFichier;

    private Long tailleFichier;

    private String mimeType;

    @Column(nullable = false)
    private LocalDateTime dateUpload;

    private Boolean valide = false;

    @Column(length = 500)
    private String commentaire;

    @PrePersist
    protected void onCreate() {
        dateUpload = LocalDateTime.now();
    }
}
