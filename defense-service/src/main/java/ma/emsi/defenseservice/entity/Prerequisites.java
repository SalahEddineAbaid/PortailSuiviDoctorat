package ma.emsi.defenseservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "prerequisites", indexes = {
        @Index(name = "idx_doctorant_id", columnList = "doctorant_id"),
        @Index(name = "idx_is_valid", columnList = "is_valid")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Prerequisites {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctorant_id", nullable = false)
    private Long doctorantId; // ID du doctorant (reference au user-service)

    private int journalArticles;
    private int conferences;
    private int trainingHours;

    private boolean manuscriptUploaded;
    private boolean antiPlagiarismUploaded;
    private boolean publicationsReportUploaded;
    private boolean trainingCertsUploaded;
    private boolean authorizationLetterUploaded;

    private boolean isValid;
}
