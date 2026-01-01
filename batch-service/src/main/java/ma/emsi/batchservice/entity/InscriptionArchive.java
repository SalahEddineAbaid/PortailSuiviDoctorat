package ma.emsi.batchservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing archived enrollment records.
 * Stores historical enrollment data with compression and encryption for
 * long-term retention.
 */
@Entity
@Table(name = "inscription_archive", indexes = {
        @Index(name = "idx_doctorant_id", columnList = "doctorant_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_archived_date", columnList = "archived_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscriptionArchive {

    @Id
    private Long id; // Same as original inscription ID

    @Column(name = "doctorant_id", nullable = false)
    private Long doctorantId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "date_validation")
    private LocalDate dateValidation;

    @Column(name = "date_rejection")
    private LocalDate dateRejection;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "date_premiere_inscription")
    private LocalDate datePremiereInscription;

    @Column(name = "annee_universitaire", length = 20)
    private String anneeUniversitaire;

    @Column(name = "discipline", length = 100)
    private String discipline;

    @Column(name = "laboratoire", length = 100)
    private String laboratoire;

    @Column(name = "directeur_these_id")
    private Long directeurTheseId;

    @Column(name = "co_directeur_these_id")
    private Long coDirecteurTheseId;

    @Column(name = "sujet_these", columnDefinition = "TEXT")
    private String sujetThese;

    @Column(name = "has_derogation")
    private Boolean hasDerogation;

    @Column(name = "derogation_motif", columnDefinition = "TEXT")
    private String derogationMotif;

    @Column(name = "derogation_date")
    private LocalDate derogationDate;

    // Archive-specific fields
    @Column(name = "archived_date", nullable = false)
    private LocalDateTime archivedDate;

    @Column(name = "archived_by", nullable = false, length = 100)
    private String archivedBy;

    @Column(name = "archive_location", nullable = false, length = 500)
    private String archiveLocation; // Path to encrypted ZIP

    @PrePersist
    protected void onCreate() {
        if (archivedDate == null) {
            archivedDate = LocalDateTime.now();
        }
        if (hasDerogation == null) {
            hasDerogation = false;
        }
    }
}
