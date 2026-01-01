package ma.emsi.batchservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Model representing a defense from defensedb.
 * Used by archive job to identify and archive completed defenses.
 * 
 * Requirements: 4.4, 4.5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Defense {

    /**
     * Unique identifier for the defense.
     */
    private Long id;

    /**
     * ID of the associated enrollment.
     */
    private Long inscriptionId;

    /**
     * Date of the defense.
     */
    private LocalDate defenseDate;

    /**
     * Time of the defense.
     */
    private LocalTime defenseTime;

    /**
     * Location where defense takes place.
     */
    private String location;

    /**
     * Mention/grade awarded (e.g., "Très Honorable", "Honorable").
     */
    private String mention;

    /**
     * ID of the jury.
     */
    private Long juryId;

    /**
     * Indicates if the PV (procès-verbal) has been signed.
     */
    private Boolean pvSigned;

    /**
     * File path to the signed PV document.
     */
    private String pvFilePath;

    /**
     * File path to the defense report.
     */
    private String rapportFilePath;

    /**
     * Current status of the defense.
     * Values: PENDING, SCHEDULED, COMPLETED, CANCELLED
     */
    private String status;

    /**
     * Timestamp when defense record was created.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when defense record was last updated.
     */
    private LocalDateTime updatedAt;

    /**
     * Indicates if the defense has been archived.
     */
    private Boolean archived;
}
