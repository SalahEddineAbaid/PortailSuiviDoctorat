package ma.emsi.batchservice.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Tasklet for optimizing database tables after archiving operations.
 * 
 * Operations performed:
 * - Execute OPTIMIZE TABLE on affected tables
 * - Regenerate table statistics
 * - Verify index integrity
 * - Log optimization results
 * 
 * Requirements: 4.8
 */
@Component
public class DatabaseOptimizationTasklet implements Tasklet {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseOptimizationTasklet.class);

    private final JdbcTemplate inscriptionJdbcTemplate;
    private final JdbcTemplate defenseJdbcTemplate;
    private final JdbcTemplate batchJdbcTemplate;

    // Tables to optimize after archiving
    private static final List<String> INSCRIPTION_TABLES = Arrays.asList(
            "inscription",
            "inscription_archive");

    private static final List<String> DEFENSE_TABLES = Arrays.asList(
            "defense",
            "defense_archive");

    private static final List<String> BATCH_TABLES = Arrays.asList(
            "archive_audit_trail",
            "job_execution_history");

    public DatabaseOptimizationTasklet(
            @Qualifier("inscriptionJdbcTemplate") JdbcTemplate inscriptionJdbcTemplate,
            @Qualifier("defenseJdbcTemplate") JdbcTemplate defenseJdbcTemplate,
            @Qualifier("batchJdbcTemplate") JdbcTemplate batchJdbcTemplate) {
        this.inscriptionJdbcTemplate = inscriptionJdbcTemplate;
        this.defenseJdbcTemplate = defenseJdbcTemplate;
        this.batchJdbcTemplate = batchJdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        logger.info("Starting database optimization tasklet");

        int totalTablesOptimized = 0;

        // Optimize inscription database tables
        totalTablesOptimized += optimizeTables(inscriptionJdbcTemplate, INSCRIPTION_TABLES, "inscriptiondb");

        // Optimize defense database tables
        totalTablesOptimized += optimizeTables(defenseJdbcTemplate, DEFENSE_TABLES, "defensedb");

        // Optimize batch database tables
        totalTablesOptimized += optimizeTables(batchJdbcTemplate, BATCH_TABLES, "batchdb");

        // Store metrics in execution context
        chunkContext.getStepContext()
                .getStepExecution()
                .getExecutionContext()
                .putInt("tablesOptimized", totalTablesOptimized);

        logger.info("Database optimization completed: {} tables optimized", totalTablesOptimized);

        return RepeatStatus.FINISHED;
    }

    /**
     * Optimizes a list of tables in a specific database.
     * 
     * @param jdbcTemplate JdbcTemplate for the database
     * @param tables       List of table names to optimize
     * @param databaseName Name of the database (for logging)
     * @return Number of tables successfully optimized
     */
    private int optimizeTables(JdbcTemplate jdbcTemplate, List<String> tables, String databaseName) {
        int optimizedCount = 0;

        for (String tableName : tables) {
            try {
                logger.info("Optimizing table {}.{}", databaseName, tableName);

                // Execute OPTIMIZE TABLE
                long startTime = System.currentTimeMillis();
                jdbcTemplate.execute("OPTIMIZE TABLE " + tableName);
                long duration = System.currentTimeMillis() - startTime;

                logger.info("Optimized table {}.{} in {} ms", databaseName, tableName, duration);

                // Analyze table to regenerate statistics
                jdbcTemplate.execute("ANALYZE TABLE " + tableName);
                logger.debug("Analyzed table {}.{}", databaseName, tableName);

                // Check table integrity
                verifyTableIntegrity(jdbcTemplate, tableName, databaseName);

                optimizedCount++;
            } catch (Exception e) {
                logger.error("Failed to optimize table {}.{}: {}",
                        databaseName, tableName, e.getMessage(), e);
                // Continue with other tables even if one fails
            }
        }

        return optimizedCount;
    }

    /**
     * Verifies the integrity of a table's indexes.
     * 
     * @param jdbcTemplate JdbcTemplate for the database
     * @param tableName    Name of the table to verify
     * @param databaseName Name of the database (for logging)
     */
    private void verifyTableIntegrity(JdbcTemplate jdbcTemplate, String tableName, String databaseName) {
        try {
            // Check table for errors
            List<String> checkResults = jdbcTemplate.query(
                    "CHECK TABLE " + tableName,
                    (rs, rowNum) -> rs.getString("Msg_text"));

            boolean hasErrors = checkResults.stream()
                    .anyMatch(msg -> msg.toLowerCase().contains("error") ||
                            msg.toLowerCase().contains("corrupt"));

            if (hasErrors) {
                logger.warn("Table {}.{} has integrity issues: {}",
                        databaseName, tableName, checkResults);
            } else {
                logger.debug("Table {}.{} integrity verified successfully", databaseName, tableName);
            }
        } catch (Exception e) {
            logger.error("Failed to verify integrity of table {}.{}: {}",
                    databaseName, tableName, e.getMessage());
        }
    }
}
