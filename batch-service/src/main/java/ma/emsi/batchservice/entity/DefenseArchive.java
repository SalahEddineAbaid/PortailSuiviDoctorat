package ma.emsi.batchservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity representing archived defense records.
 * Stores historical defense data with compression and encryption for long-term
 * retention.
 */
@Entity
@Table(name = "defense_archive", indexes = {
        @Index(name = "idx_inscription_id", columnList = "inscription_id"),
        @Index(name = "idx_defense_date", columnList = "defense_date"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_archived_date", columnList = "archived_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefenseArchive {

    @Id
    private Long id; // Same as original defense ID

    @Column(name = "inscription_id", nullable = false)
    private Long inscriptionId;

    @Column(name = "defense_date", nullable = false)
    private LocalDate defenseDate;

    @Column(name = "defense_time")
    private LocalTime defenseTime;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "mention", length = 50)
    private String mention;

    @Column(name = "jury_id")
    private Long juryId;

    @Column(name = "pv_signed")
    private Boolean pvSigned;

    @Column(name = "pv_file_path", length = 500)
    private String pvFilePath;

    @Column(name = "rapport_file_path", length = 500)
    private String rapportFilePath;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
        if (pvSigned == null) {
            pvSigned = false;
        }
    }
}
