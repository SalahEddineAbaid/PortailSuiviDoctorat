package ma.emsi.batchservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Model representing an enrollment (inscription) from inscriptiondb.
 * Used by duration alert job to identify doctorants approaching critical
 * duration thresholds, and by archive job to archive old enrollments.
 * 
 * This model contains fields needed for duration alert processing and
 * archiving.
 * 
 * Requirements: 2.2, 2.4, 2.8, 4.2, 4.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscription {

    /**
     * Unique identifier for the enrollment.
     */
    private Long id;

    /**
     * ID of the doctorant (PhD student).
     */
    private Long doctorantId;

    /**
     * Date of first enrollment in the doctoral program.
     * Used to calculate duration and determine alert thresholds.
     */
    private LocalDate datePremiereInscription;

    /**
     * Current status of the enrollment.
     * Values: EN_ATTENTE, VALIDÉ, REJETÉ, SUSPENDU, BLOQUÉ
     */
    private String statut;

    /**
     * Current status of the enrollment (alternative field name for compatibility).
     * Values: EN_ATTENTE, VALIDÉ, REJETÉ, SUSPENDU, BLOQUÉ
     */
    private String status;

    /**
     * Email of the doctorant.
     */
    private String doctorantEmail;

    /**
     * Full name of the doctorant.
     */
    private String doctorantNom;

    /**
     * Email of the thesis director.
     */
    private String directeurEmail;

    /**
     * Indicates if a dérogation (extension) has been granted.
     */
    private Boolean derogationAccordee;

    /**
     * Indicates if an exceptional dérogation (beyond 6 years) has been granted.
     */
    private Boolean derogationExceptionnelle;

    // Additional fields for archiving

    /**
     * Date when enrollment was validated.
     */
    private LocalDate dateValidation;

    /**
     * Date when enrollment was rejected.
     */
    private LocalDate dateRejection;

    /**
     * Reason for rejection if status is REJETÉ.
     */
    private String motifRefus;

    /**
     * Academic year of enrollment.
     */
    private String anneeUniversitaire;

    /**
     * Discipline/field of study.
     */
    private String discipline;

    /**
     * Laboratory/research unit.
     */
    private String laboratoire;

    /**
     * ID of the thesis director.
     */
    private Long directeurTheseId;

    /**
     * ID of the co-director (if any).
     */
    private Long coDirecteurTheseId;

    /**
     * Thesis subject/title.
     */
    private String sujetThese;

    /**
     * Indicates if a dérogation has been granted.
     */
    private Boolean hasDerogation;

    /**
     * Reason for dérogation.
     */
    private String derogationMotif;

    /**
     * Date when dérogation was granted.
     */
    private LocalDate derogationDate;

    /**
     * Indicates if the enrollment has been archived.
     */
    private Boolean archived;
}
