package ma.emsi.batchservice.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Tasklet for checking and managing orphaned documents in the uploads
 * directory.
 * 
 * Orphaned Document Detection:
 * - List all files in uploads directory
 * - Query database for file references across all services
 * - Identify files without database references
 * 
 * Corrective Actions:
 * - Move orphaned files to uploads/orphelins directory
 * - Log orphaned file list for audit
 * - Track orphan count for metrics
 * 
 * Database Tables Checked:
 * - inscription.document_path (inscriptiondb)
 * - defense.document_path, defense.pv_path (defensedb)
 * - defense_request.document_path (defensedb)
 * - document table (defensedb)
 * 
 * Requirements: 5.8, 5.9
 */
@Slf4j
@Component
public class CheckOrphanedDocumentsTasklet implements Tasklet {

    @Value("${batch.uploads.directory:uploads}")
    private String uploadsDirectory;

    private final JdbcTemplate inscriptionJdbcTemplate;
    private final JdbcTemplate defenseJdbcTemplate;

    public CheckOrphanedDocumentsTasklet(
            @Qualifier("inscriptionJdbcTemplate") JdbcTemplate inscriptionJdbcTemplate,
            @Qualifier("defenseJdbcTemplate") JdbcTemplate defenseJdbcTemplate) {
        this.inscriptionJdbcTemplate = inscriptionJdbcTemplate;
        this.defenseJdbcTemplate = defenseJdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting orphaned documents check in directory: {}", uploadsDirectory);

        int orphanCount = 0;
        List<String> orphanedFiles = new ArrayList<>();

        try {
            Path uploadsPath = Paths.get(uploadsDirectory);

            // Ensure uploads directory exists
            if (!Files.exists(uploadsPath)) {
                log.warn("Uploads directory does not exist: {}", uploadsDirectory);
                return RepeatStatus.FINISHED;
            }

            // Create orphelins directory if it doesn't exist
            Path orphelinsPath = uploadsPath.resolve("orphelins");
            if (!Files.exists(orphelinsPath)) {
                Files.createDirectories(orphelinsPath);
                log.info("Created orphelins directory: {}", orphelinsPath);
            }

            // Collect all file paths from database
            Set<String> referencedFiles = collectReferencedFiles();
            log.info("Found {} file references in database", referencedFiles.size());

            // List all files in uploads directory (recursively)
            try (Stream<Path> paths = Files.walk(uploadsPath)) {
                List<Path> allFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> !p.startsWith(orphelinsPath)) // Exclude orphelins directory
                        .toList();

                log.info("Found {} files in uploads directory", allFiles.size());

                for (Path filePath : allFiles) {
                    String relativePath = uploadsPath.relativize(filePath).toString();
                    String normalizedPath = relativePath.replace("\\", "/");

                    // Check if file is referenced in database
                    boolean isReferenced = referencedFiles.stream()
                            .anyMatch(ref -> ref != null && (ref.contains(normalizedPath) ||
                                    normalizedPath.contains(ref) ||
                                    ref.endsWith(filePath.getFileName().toString())));

                    if (!isReferenced) {
                        // File is orphaned - move to orphelins directory
                        orphanCount++;
                        orphanedFiles.add(normalizedPath);

                        try {
                            Path targetPath = orphelinsPath.resolve(filePath.getFileName());
                            // If file with same name exists, append timestamp
                            if (Files.exists(targetPath)) {
                                String fileName = filePath.getFileName().toString();
                                String timestamp = String.valueOf(System.currentTimeMillis());
                                targetPath = orphelinsPath.resolve(timestamp + "_" + fileName);
                            }

                            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            log.info("Moved orphaned file: {} -> {}", relativePath, targetPath.getFileName());
                        } catch (IOException e) {
                            log.error("Failed to move orphaned file: {}", relativePath, e);
                        }
                    }
                }
            }

            // Log orphaned files list
            if (!orphanedFiles.isEmpty()) {
                log.warn("Orphaned files detected and moved: {}", orphanedFiles);
            } else {
                log.info("No orphaned files detected");
            }

            // Store metrics in execution context for listener
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("orphanedDocuments", orphanCount);
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putString("orphanedFilesList", String.join(", ", orphanedFiles));

            log.info("Orphaned documents check completed. Orphans found: {}", orphanCount);

        } catch (Exception e) {
            log.error("Error during orphaned documents check", e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * Collects all file references from database tables across services.
     */
    private Set<String> collectReferencedFiles() {
        Set<String> referencedFiles = new HashSet<>();

        // Collect from inscription documents
        try {
            String inscriptionQuery = """
                    SELECT document_path
                    FROM inscription
                    WHERE document_path IS NOT NULL
                    """;
            List<String> inscriptionDocs = inscriptionJdbcTemplate.queryForList(inscriptionQuery, String.class);
            referencedFiles.addAll(inscriptionDocs);
            log.debug("Found {} document references in inscription table", inscriptionDocs.size());
        } catch (Exception e) {
            log.warn("Failed to query inscription documents: {}", e.getMessage());
        }

        // Collect from defense documents
        try {
            String defenseQuery = """
                    SELECT document_path
                    FROM defense
                    WHERE document_path IS NOT NULL
                    UNION
                    SELECT pv_path
                    FROM defense
                    WHERE pv_path IS NOT NULL
                    """;
            List<String> defenseDocs = defenseJdbcTemplate.queryForList(defenseQuery, String.class);
            referencedFiles.addAll(defenseDocs);
            log.debug("Found {} document references in defense table", defenseDocs.size());
        } catch (Exception e) {
            log.warn("Failed to query defense documents: {}", e.getMessage());
        }

        // Collect from defense_request documents
        try {
            String defenseRequestQuery = """
                    SELECT document_path
                    FROM defense_request
                    WHERE document_path IS NOT NULL
                    """;
            List<String> defenseRequestDocs = defenseJdbcTemplate.queryForList(defenseRequestQuery, String.class);
            referencedFiles.addAll(defenseRequestDocs);
            log.debug("Found {} document references in defense_request table", defenseRequestDocs.size());
        } catch (Exception e) {
            log.warn("Failed to query defense_request documents: {}", e.getMessage());
        }

        // Collect from document table
        try {
            String documentQuery = """
                    SELECT file_path
                    FROM document
                    WHERE file_path IS NOT NULL
                    """;
            List<String> documentPaths = defenseJdbcTemplate.queryForList(documentQuery, String.class);
            referencedFiles.addAll(documentPaths);
            log.debug("Found {} document references in document table", documentPaths.size());
        } catch (Exception e) {
            log.warn("Failed to query document table: {}", e.getMessage());
        }

        return referencedFiles;
    }
}
