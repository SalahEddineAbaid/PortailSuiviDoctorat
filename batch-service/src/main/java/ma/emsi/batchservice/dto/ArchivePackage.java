package ma.emsi.batchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a complete archive package ready for writing.
 * Contains the archive entity, encrypted file data, and audit information.
 * 
 * Requirements: 4.3, 4.5, 4.10, 4.11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivePackage {

    /**
     * Type of archive: ENROLLMENT or DEFENSE
     */
    private ArchiveType archiveType;

    /**
     * Original record ID
     */
    private Long originalId;

    /**
     * Archive entity (InscriptionArchive or DefenseArchive)
     */
    private Object archiveEntity;

    /**
     * Encrypted ZIP file data
     */
    private byte[] encryptedZipData;

    /**
     * Path where encrypted ZIP should be stored
     */
    private String archiveLocation;

    /**
     * List of original document file paths to be deleted after archiving
     */
    private List<String> originalDocumentPaths;

    /**
     * Audit trail information
     */
    private String archivedBy;
    private LocalDateTime archivedDate;

    /**
     * Size of uncompressed data (for metrics)
     */
    private long uncompressedSize;

    /**
     * Size of compressed and encrypted data (for metrics)
     */
    private long compressedSize;

    public enum ArchiveType {
        ENROLLMENT,
        DEFENSE
    }
}
