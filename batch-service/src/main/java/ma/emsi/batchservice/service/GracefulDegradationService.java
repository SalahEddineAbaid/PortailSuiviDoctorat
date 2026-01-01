package ma.emsi.batchservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for handling graceful degradation scenarios.
 * 
 * Handles:
 * - Database connection failures
 * - Kafka broker unavailability
 * - Disk space exhaustion
 * - File permission errors
 * 
 * Provides health checks and fallback mechanisms to prevent complete system
 * failure.
 * 
 * Requirements: Error Handling section
 */
@Service
public class GracefulDegradationService {

    private static final Logger logger = LoggerFactory.getLogger(GracefulDegradationService.class);

    private final FailureNotificationService failureNotificationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${batch.graceful-degradation.min-disk-space-gb:10}")
    private long minDiskSpaceGB;

    @Value("${batch.graceful-degradation.kafka-timeout-ms:5000}")
    private long kafkaTimeoutMs;

    @Value("${batch.graceful-degradation.db-connection-timeout-seconds:30}")
    private int dbConnectionTimeoutSeconds;

    @Value("${batch.paths.archives:./archives}")
    private String archivesPath;

    @Value("${batch.paths.reports:./reports}")
    private String reportsPath;

    @Value("${batch.paths.uploads:./uploads}")
    private String uploadsPath;

    // Cache for health check results to avoid excessive checks
    private final Map<String, HealthCheckResult> healthCheckCache = new HashMap<>();
    private static final long CACHE_VALIDITY_MS = 60000; // 1 minute

    public GracefulDegradationService(
            FailureNotificationService failureNotificationService,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.failureNotificationService = failureNotificationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Checks if a database connection is available.
     * 
     * @param dataSource DataSource to check
     * @param dbName     Name of the database (for logging)
     * @return true if connection is available, false otherwise
     */
    public boolean isDatabaseAvailable(DataSource dataSource, String dbName) {
        String cacheKey = "db_" + dbName;
        HealthCheckResult cached = healthCheckCache.get(cacheKey);

        if (cached != null && cached.isValid()) {
            return cached.isHealthy();
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(dbConnectionTimeoutSeconds);
            healthCheckCache.put(cacheKey, new HealthCheckResult(isValid));

            if (!isValid) {
                logger.error("Database connection validation failed for {}", dbName);
                notifyInfrastructureFailure("Database", dbName, "Connection validation failed");
            }

            return isValid;
        } catch (SQLException e) {
            logger.error("Failed to connect to database {}: {}", dbName, e.getMessage());
            healthCheckCache.put(cacheKey, new HealthCheckResult(false));
            notifyInfrastructureFailure("Database", dbName, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if Kafka broker is available.
     * 
     * @return true if Kafka is available, false otherwise
     */
    public boolean isKafkaAvailable() {
        String cacheKey = "kafka";
        HealthCheckResult cached = healthCheckCache.get(cacheKey);

        if (cached != null && cached.isValid()) {
            return cached.isHealthy();
        }

        try {
            // Send a test message with timeout
            kafkaTemplate.send("health-check", "test", "ping")
                    .get(kafkaTimeoutMs, TimeUnit.MILLISECONDS);

            healthCheckCache.put(cacheKey, new HealthCheckResult(true));
            return true;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Kafka broker unavailable: {}", e.getMessage());
            healthCheckCache.put(cacheKey, new HealthCheckResult(false));
            notifyInfrastructureFailure("Kafka", "broker", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if sufficient disk space is available.
     * 
     * @param path Path to check
     * @return true if sufficient space is available, false otherwise
     */
    public boolean hasSufficientDiskSpace(String path) {
        try {
            Path dirPath = Paths.get(path);

            // Create directory if it doesn't exist
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            FileStore store = Files.getFileStore(dirPath);
            long usableSpaceGB = store.getUsableSpace() / (1024 * 1024 * 1024);

            if (usableSpaceGB < minDiskSpaceGB) {
                logger.error("Insufficient disk space at {}: {} GB available, {} GB required",
                        path, usableSpaceGB, minDiskSpaceGB);
                notifyInfrastructureFailure("Disk Space", path,
                        String.format("Only %d GB available, %d GB required", usableSpaceGB, minDiskSpaceGB));
                return false;
            }

            return true;
        } catch (IOException e) {
            logger.error("Failed to check disk space at {}: {}", path, e.getMessage());
            notifyInfrastructureFailure("Disk Space", path, e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a directory has proper read/write permissions.
     * 
     * @param path Path to check
     * @return true if permissions are correct, false otherwise
     */
    public boolean hasProperFilePermissions(String path) {
        try {
            Path dirPath = Paths.get(path);

            // Create directory if it doesn't exist
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            File dir = dirPath.toFile();

            if (!dir.canRead()) {
                logger.error("No read permission for directory: {}", path);
                notifyInfrastructureFailure("File Permissions", path, "No read permission");
                return false;
            }

            if (!dir.canWrite()) {
                logger.error("No write permission for directory: {}", path);
                notifyInfrastructureFailure("File Permissions", path, "No write permission");
                return false;
            }

            // Try to create a test file
            Path testFile = dirPath.resolve(".permission_test");
            try {
                Files.write(testFile, "test".getBytes());
                Files.delete(testFile);
            } catch (IOException e) {
                logger.error("Failed to write test file in {}: {}", path, e.getMessage());
                notifyInfrastructureFailure("File Permissions", path,
                        "Cannot write files: " + e.getMessage());
                return false;
            }

            return true;
        } catch (IOException e) {
            logger.error("Failed to check file permissions for {}: {}", path, e.getMessage());
            notifyInfrastructureFailure("File Permissions", path, e.getMessage());
            return false;
        }
    }

    /**
     * Performs pre-flight checks before starting an archive job.
     * 
     * @return true if all checks pass, false otherwise
     */
    public boolean performArchivePreflightChecks() {
        logger.info("Performing archive job preflight checks...");

        boolean allChecksPass = true;

        // Check disk space
        if (!hasSufficientDiskSpace(archivesPath)) {
            logger.error("Archive preflight check failed: insufficient disk space");
            allChecksPass = false;
        }

        // Check file permissions
        if (!hasProperFilePermissions(archivesPath)) {
            logger.error("Archive preflight check failed: improper file permissions");
            allChecksPass = false;
        }

        if (!hasProperFilePermissions(uploadsPath)) {
            logger.error("Archive preflight check failed: cannot access uploads directory");
            allChecksPass = false;
        }

        if (allChecksPass) {
            logger.info("Archive preflight checks passed");
        } else {
            logger.error("Archive preflight checks failed - job should not proceed");
        }

        return allChecksPass;
    }

    /**
     * Performs pre-flight checks before starting a report generation job.
     * 
     * @return true if all checks pass, false otherwise
     */
    public boolean performReportPreflightChecks() {
        logger.info("Performing report generation preflight checks...");

        boolean allChecksPass = true;

        // Check disk space
        if (!hasSufficientDiskSpace(reportsPath)) {
            logger.error("Report preflight check failed: insufficient disk space");
            allChecksPass = false;
        }

        // Check file permissions
        if (!hasProperFilePermissions(reportsPath)) {
            logger.error("Report preflight check failed: improper file permissions");
            allChecksPass = false;
        }

        if (allChecksPass) {
            logger.info("Report preflight checks passed");
        } else {
            logger.error("Report preflight checks failed - job should not proceed");
        }

        return allChecksPass;
    }

    /**
     * Handles database connection failure gracefully.
     * 
     * @param dbName  Name of the database
     * @param jobName Name of the job that encountered the failure
     */
    public void handleDatabaseFailure(String dbName, String jobName) {
        logger.error("Database connection failure detected for {} in job {}", dbName, jobName);

        // Clear cache to force recheck on next attempt
        healthCheckCache.remove("db_" + dbName);

        // Notify about the failure
        notifyInfrastructureFailure("Database", dbName,
                String.format("Connection failed during job %s", jobName));
    }

    /**
     * Handles Kafka broker unavailability gracefully.
     * 
     * @param jobName Name of the job that encountered the failure
     */
    public void handleKafkaFailure(String jobName) {
        logger.error("Kafka broker unavailable in job {}", jobName);

        // Clear cache to force recheck on next attempt
        healthCheckCache.remove("kafka");

        // Notify about the failure
        notifyInfrastructureFailure("Kafka", "broker",
                String.format("Broker unavailable during job %s", jobName));
    }

    /**
     * Notifies about infrastructure failures.
     */
    private void notifyInfrastructureFailure(String component, String resource, String message) {
        try {
            String errorMessage = String.format("Infrastructure failure: %s - %s: %s",
                    component, resource, message);

            failureNotificationService.notifyJobFailure(
                    "Infrastructure Check",
                    0L,
                    java.time.LocalDateTime.now(),
                    new RuntimeException(errorMessage),
                    errorMessage);
        } catch (Exception e) {
            logger.error("Failed to send infrastructure failure notification: {}", e.getMessage());
        }
    }

    /**
     * Inner class to cache health check results.
     */
    private static class HealthCheckResult {
        private final boolean healthy;
        private final long timestamp;

        public HealthCheckResult(boolean healthy) {
            this.healthy = healthy;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isHealthy() {
            return healthy;
        }

        public boolean isValid() {
            return (System.currentTimeMillis() - timestamp) < CACHE_VALIDITY_MS;
        }
    }
}
