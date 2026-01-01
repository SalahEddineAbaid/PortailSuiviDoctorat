package ma.emsi.batchservice.processor;

import ma.emsi.batchservice.dto.ArchivePackage;
import ma.emsi.batchservice.entity.DefenseArchive;
import ma.emsi.batchservice.entity.InscriptionArchive;
import ma.emsi.batchservice.model.Defense;
import ma.emsi.batchservice.model.Inscription;
import ma.emsi.batchservice.service.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Processor for archiving enrollment and defense records.
 * 
 * Processing steps:
 * 1. Copy record data to archive entity
 * 2. Locate associated documents in file system
 * 3. Compress documents to ZIP format
 * 4. Encrypt ZIP file with AES-256
 * 5. Generate archive location path
 * 6. Prepare audit trail entry
 * 
 * Requirements: 4.3, 4.5, 4.10, 4.11
 */
@Component
public class ArchiveProcessor implements ItemProcessor<Object, ArchivePackage> {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveProcessor.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final EncryptionService encryptionService;

    @Value("${batch.uploads.directory:./uploads}")
    private String uploadsDirectory;

    @Value("${batch.archives.directory:./archives}")
    private String archivesDirectory;

    public ArchiveProcessor(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public ArchivePackage process(Object item) throws Exception {
        if (item instanceof Inscription) {
            return processEnrollment((Inscription) item);
        } else if (item instanceof Defense) {
            return processDefense((Defense) item);
        } else {
            logger.error("Unknown item type for archiving: {}", item.getClass().getName());
            return null;
        }
    }

    /**
     * Processes an enrollment for archiving.
     */
    private ArchivePackage processEnrollment(Inscription inscription) throws Exception {
        logger.info("Processing enrollment {} for archiving", inscription.getId());

        // Create archive entity
        InscriptionArchive archive = InscriptionArchive.builder()
                .id(inscription.getId())
                .doctorantId(inscription.getDoctorantId())
                .status(inscription.getStatus())
                .dateValidation(inscription.getDateValidation())
                .dateRejection(inscription.getDateRejection())
                .motifRefus(inscription.getMotifRefus())
                .datePremiereInscription(inscription.getDatePremiereInscription())
                .anneeUniversitaire(inscription.getAnneeUniversitaire())
                .discipline(inscription.getDiscipline())
                .laboratoire(inscription.getLaboratoire())
                .directeurTheseId(inscription.getDirecteurTheseId())
                .coDirecteurTheseId(inscription.getCoDirecteurTheseId())
                .sujetThese(inscription.getSujetThese())
                .hasDerogation(inscription.getHasDerogation())
                .derogationMotif(inscription.getDerogationMotif())
                .derogationDate(inscription.getDerogationDate())
                .archivedDate(LocalDateTime.now())
                .archivedBy("SYSTEM")
                .build();

        // Locate associated documents
        List<String> documentPaths = locateEnrollmentDocuments(inscription.getId());

        // Compress documents to ZIP
        byte[] zipData = compressDocuments(documentPaths, "enrollment_" + inscription.getId());
        long uncompressedSize = calculateTotalSize(documentPaths);

        // Encrypt ZIP file
        byte[] encryptedData = encryptionService.encrypt(zipData);

        // Generate archive location
        String archiveLocation = generateArchiveLocation("enrollment", inscription.getId());
        archive.setArchiveLocation(archiveLocation);

        // Build archive package
        return ArchivePackage.builder()
                .archiveType(ArchivePackage.ArchiveType.ENROLLMENT)
                .originalId(inscription.getId())
                .archiveEntity(archive)
                .encryptedZipData(encryptedData)
                .archiveLocation(archiveLocation)
                .originalDocumentPaths(documentPaths)
                .archivedBy("SYSTEM")
                .archivedDate(LocalDateTime.now())
                .uncompressedSize(uncompressedSize)
                .compressedSize(encryptedData.length)
                .build();
    }

    /**
     * Processes a defense for archiving.
     */
    private ArchivePackage processDefense(Defense defense) throws Exception {
        logger.info("Processing defense {} for archiving", defense.getId());

        // Create archive entity
        DefenseArchive archive = DefenseArchive.builder()
                .id(defense.getId())
                .inscriptionId(defense.getInscriptionId())
                .defenseDate(defense.getDefenseDate())
                .defenseTime(defense.getDefenseTime())
                .location(defense.getLocation())
                .mention(defense.getMention())
                .juryId(defense.getJuryId())
                .pvSigned(defense.getPvSigned())
                .pvFilePath(defense.getPvFilePath())
                .rapportFilePath(defense.getRapportFilePath())
                .status(defense.getStatus())
                .createdAt(defense.getCreatedAt())
                .updatedAt(defense.getUpdatedAt())
                .archivedDate(LocalDateTime.now())
                .archivedBy("SYSTEM")
                .build();

        // Locate associated documents
        List<String> documentPaths = locateDefenseDocuments(defense);

        // Compress documents to ZIP
        byte[] zipData = compressDocuments(documentPaths, "defense_" + defense.getId());
        long uncompressedSize = calculateTotalSize(documentPaths);

        // Encrypt ZIP file
        byte[] encryptedData = encryptionService.encrypt(zipData);

        // Generate archive location
        String archiveLocation = generateArchiveLocation("defense", defense.getId());
        archive.setArchiveLocation(archiveLocation);

        // Build archive package
        return ArchivePackage.builder()
                .archiveType(ArchivePackage.ArchiveType.DEFENSE)
                .originalId(defense.getId())
                .archiveEntity(archive)
                .encryptedZipData(encryptedData)
                .archiveLocation(archiveLocation)
                .originalDocumentPaths(documentPaths)
                .archivedBy("SYSTEM")
                .archivedDate(LocalDateTime.now())
                .uncompressedSize(uncompressedSize)
                .compressedSize(encryptedData.length)
                .build();
    }

    /**
     * Locates all documents associated with an enrollment.
     */
    private List<String> locateEnrollmentDocuments(Long enrollmentId) {
        List<String> documentPaths = new ArrayList<>();

        // Look for enrollment-related documents in uploads directory
        String enrollmentDir = uploadsDirectory + "/enrollments/" + enrollmentId;
        File dir = new File(enrollmentDir);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        documentPaths.add(file.getAbsolutePath());
                    }
                }
            }
        }

        logger.debug("Found {} documents for enrollment {}", documentPaths.size(), enrollmentId);
        return documentPaths;
    }

    /**
     * Locates all documents associated with a defense.
     */
    private List<String> locateDefenseDocuments(Defense defense) {
        List<String> documentPaths = new ArrayList<>();

        // Add PV file if exists
        if (defense.getPvFilePath() != null && !defense.getPvFilePath().isEmpty()) {
            File pvFile = new File(uploadsDirectory + "/" + defense.getPvFilePath());
            if (pvFile.exists()) {
                documentPaths.add(pvFile.getAbsolutePath());
            }
        }

        // Add rapport file if exists
        if (defense.getRapportFilePath() != null && !defense.getRapportFilePath().isEmpty()) {
            File rapportFile = new File(uploadsDirectory + "/" + defense.getRapportFilePath());
            if (rapportFile.exists()) {
                documentPaths.add(rapportFile.getAbsolutePath());
            }
        }

        // Look for other defense-related documents
        String defenseDir = uploadsDirectory + "/defenses/" + defense.getId();
        File dir = new File(defenseDir);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && !documentPaths.contains(file.getAbsolutePath())) {
                        documentPaths.add(file.getAbsolutePath());
                    }
                }
            }
        }

        logger.debug("Found {} documents for defense {}", documentPaths.size(), defense.getId());
        return documentPaths;
    }

    /**
     * Compresses a list of documents into a ZIP file.
     */
    private byte[] compressDocuments(List<String> documentPaths, String zipBaseName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String documentPath : documentPaths) {
                File file = new File(documentPath);
                if (!file.exists()) {
                    logger.warn("Document not found: {}", documentPath);
                    continue;
                }

                // Create ZIP entry with relative path
                String entryName = file.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                zos.putNextEntry(zipEntry);

                // Write file content
                byte[] fileContent = Files.readAllBytes(file.toPath());
                zos.write(fileContent);
                zos.closeEntry();

                logger.debug("Added {} to ZIP ({} bytes)", entryName, fileContent.length);
            }
        }

        byte[] zipData = baos.toByteArray();
        logger.info("Created ZIP archive {} with {} files ({} bytes)",
                zipBaseName, documentPaths.size(), zipData.length);
        return zipData;
    }

    /**
     * Calculates total size of all documents.
     */
    private long calculateTotalSize(List<String> documentPaths) {
        long totalSize = 0;
        for (String path : documentPaths) {
            File file = new File(path);
            if (file.exists()) {
                totalSize += file.length();
            }
        }
        return totalSize;
    }

    /**
     * Generates the archive location path.
     */
    private String generateArchiveLocation(String type, Long id) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String year = String.valueOf(LocalDateTime.now().getYear());
        String month = String.format("%02d", LocalDateTime.now().getMonthValue());

        return String.format("%s/%s/%s/%s_%s_%s.zip.enc",
                archivesDirectory, year, month, type, id, timestamp);
    }
}
