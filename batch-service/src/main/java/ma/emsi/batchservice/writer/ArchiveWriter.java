package ma.emsi.batchservice.writer;

import ma.emsi.batchservice.dto.ArchivePackage;
import ma.emsi.batchservice.entity.DefenseArchive;
import ma.emsi.batchservice.entity.InscriptionArchive;
import ma.emsi.batchservice.repository.DefenseArchiveRepository;
import ma.emsi.batchservice.repository.InscriptionArchiveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Writer for archiving enrollment and defense records.
 * 
 * Operations performed in a single transaction:
 * 1. Insert into archive table (inscription_archive or defense_archive)
 * 2. Write encrypted ZIP to archives directory
 * 3. Insert audit trail entry
 * 4. Update original record (set archived = true)
 * 5. Delete original documents
 * 
 * If any operation fails, the entire transaction is rolled back.
 * 
 * Requirements: 4.3, 4.5, 4.11
 */
@Component
public class ArchiveWriter implements ItemWriter<ArchivePackage> {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveWriter.class);

    private final InscriptionArchiveRepository inscriptionArchiveRepository;
    private final DefenseArchiveRepository defenseArchiveRepository;
    private final JdbcTemplate inscriptionJdbcTemplate;
    private final JdbcTemplate defenseJdbcTemplate;
    private final JdbcTemplate batchJdbcTemplate;

    public ArchiveWriter(
            InscriptionArchiveRepository inscriptionArchiveRepository,
            DefenseArchiveRepository defenseArchiveRepository,
            @Qualifier("inscriptionJdbcTemplate") JdbcTemplate inscriptionJdbcTemplate,
            @Qualifier("defenseJdbcTemplate") JdbcTemplate defenseJdbcTemplate,
            @Qualifier("batchJdbcTemplate") JdbcTemplate batchJdbcTemplate) {
        this.inscriptionArchiveRepository = inscriptionArchiveRepository;
        this.defenseArchiveRepository = defenseArchiveRepository;
        this.inscriptionJdbcTemplate = inscriptionJdbcTemplate;
        this.defenseJdbcTemplate = defenseJdbcTemplate;
        this.batchJdbcTemplate = batchJdbcTemplate;
    }

    @Override
    @Transactional
    public void write(Chunk<? extends ArchivePackage> chunk) throws Exception {
        List<? extends ArchivePackage> items = chunk.getItems();

        for (ArchivePackage archivePackage : items) {
            try {
                writeArchivePackage(archivePackage);
            } catch (Exception e) {
                logger.error("Failed to archive {} with ID {}: {}",
                        archivePackage.getArchiveType(),
                        archivePackage.getOriginalId(),
                        e.getMessage(), e);
                throw e; // Rollback transaction
            }
        }
    }

    /**
     * Writes a single archive package.
     */
    private void writeArchivePackage(ArchivePackage archivePackage) throws Exception {
        logger.info("Writing archive package for {} ID {}",
                archivePackage.getArchiveType(), archivePackage.getOriginalId());

        // Step 1: Insert into archive table
        insertArchiveRecord(archivePackage);

        // Step 2: Write encrypted ZIP to file system
        writeEncryptedZip(archivePackage);

        // Step 3: Insert audit trail entry
        insertAuditTrail(archivePackage);

        // Step 4: Update original record (set archived = true)
        updateOriginalRecord(archivePackage);

        // Step 5: Delete original documents
        deleteOriginalDocuments(archivePackage);

        logger.info("Successfully archived {} ID {} ({} bytes compressed, {} bytes saved)",
                archivePackage.getArchiveType(),
                archivePackage.getOriginalId(),
                archivePackage.getCompressedSize(),
                archivePackage.getUncompressedSize() - archivePackage.getCompressedSize());
    }

    /**
     * Inserts the archive record into the appropriate archive table.
     */
    private void insertArchiveRecord(ArchivePackage archivePackage) {
        if (archivePackage.getArchiveType() == ArchivePackage.ArchiveType.ENROLLMENT) {
            InscriptionArchive archive = (InscriptionArchive) archivePackage.getArchiveEntity();
            inscriptionArchiveRepository.save(archive);
            logger.debug("Inserted enrollment archive record for ID {}", archive.getId());
        } else if (archivePackage.getArchiveType() == ArchivePackage.ArchiveType.DEFENSE) {
            DefenseArchive archive = (DefenseArchive) archivePackage.getArchiveEntity();
            defenseArchiveRepository.save(archive);
            logger.debug("Inserted defense archive record for ID {}", archive.getId());
        }
    }

    /**
     * Writes the encrypted ZIP file to the archives directory.
     */
    private void writeEncryptedZip(ArchivePackage archivePackage) throws IOException {
        Path archivePath = Paths.get(archivePackage.getArchiveLocation());

        // Create parent directories if they don't exist
        Files.createDirectories(archivePath.getParent());

        // Write encrypted data to file
        Files.write(archivePath, archivePackage.getEncryptedZipData(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        logger.debug("Wrote encrypted ZIP to {}", archivePath);
    }

    /**
     * Inserts an audit trail entry for the archive operation.
     */
    private void insertAuditTrail(ArchivePackage archivePackage) {
        String sql = "INSERT INTO archive_audit_trail " +
                "(entity_type, entity_id, archive_location, archived_by, archived_date, " +
                "uncompressed_size, compressed_size) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        batchJdbcTemplate.update(sql,
                archivePackage.getArchiveType().name(),
                archivePackage.getOriginalId(),
                archivePackage.getArchiveLocation(),
                archivePackage.getArchivedBy(),
                archivePackage.getArchivedDate(),
                archivePackage.getUncompressedSize(),
                archivePackage.getCompressedSize());

        logger.debug("Inserted audit trail entry for {} ID {}",
                archivePackage.getArchiveType(), archivePackage.getOriginalId());
    }

    /**
     * Updates the original record to mark it as archived.
     */
    private void updateOriginalRecord(ArchivePackage archivePackage) {
        if (archivePackage.getArchiveType() == ArchivePackage.ArchiveType.ENROLLMENT) {
            String sql = "UPDATE inscription SET archived = true WHERE id = ?";
            inscriptionJdbcTemplate.update(sql, archivePackage.getOriginalId());
            logger.debug("Updated enrollment {} archived flag", archivePackage.getOriginalId());
        } else if (archivePackage.getArchiveType() == ArchivePackage.ArchiveType.DEFENSE) {
            String sql = "UPDATE defense SET archived = true WHERE id = ?";
            defenseJdbcTemplate.update(sql, archivePackage.getOriginalId());
            logger.debug("Updated defense {} archived flag", archivePackage.getOriginalId());
        }
    }

    /**
     * Deletes the original documents from the file system.
     */
    private void deleteOriginalDocuments(ArchivePackage archivePackage) {
        int deletedCount = 0;
        int failedCount = 0;

        for (String documentPath : archivePackage.getOriginalDocumentPaths()) {
            try {
                Path path = Paths.get(documentPath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    deletedCount++;
                    logger.debug("Deleted original document: {}", documentPath);
                } else {
                    logger.warn("Document not found for deletion: {}", documentPath);
                }
            } catch (IOException e) {
                failedCount++;
                logger.error("Failed to delete document {}: {}", documentPath, e.getMessage());
                // Don't throw exception - continue with other deletions
            }
        }

        logger.info("Deleted {}/{} original documents for {} ID {} ({} failed)",
                deletedCount,
                archivePackage.getOriginalDocumentPaths().size(),
                archivePackage.getArchiveType(),
                archivePackage.getOriginalId(),
                failedCount);
    }
}
