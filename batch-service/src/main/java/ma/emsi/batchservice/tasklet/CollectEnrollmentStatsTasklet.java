package ma.emsi.batchservice.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.dto.EnrollmentStatsDTO;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tasklet to collect enrollment statistics from inscription database for the
 * previous month.
 * Stores the collected statistics in the job execution context for use by
 * subsequent steps.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectEnrollmentStatsTasklet implements Tasklet {

    @Qualifier("inscriptionJdbcTemplate")
    private final JdbcTemplate inscriptionJdbcTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting enrollment statistics collection...");

        // Calculate previous month date range
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = previousMonth.atDay(1);
        LocalDate endDate = previousMonth.atEndOfMonth();

        log.info("Collecting enrollment statistics for period: {} to {}", startDate, endDate);

        // Collect total enrollments
        Long totalEnrollments = inscriptionJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inscription WHERE DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Collect status distribution
        Map<String, Long> statusDistribution = new HashMap<>();
        List<Map<String, Object>> statusResults = inscriptionJdbcTemplate.queryForList(
                "SELECT status, COUNT(*) as count FROM inscription " +
                        "WHERE DATE(created_at) BETWEEN ? AND ? GROUP BY status",
                startDate, endDate);
        for (Map<String, Object> row : statusResults) {
            statusDistribution.put((String) row.get("status"), ((Number) row.get("count")).longValue());
        }

        // Collect reinscriptions count
        Long reinscriptionsCount = inscriptionJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inscription WHERE is_reinscription = true " +
                        "AND DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Collect dérogation statistics
        Long derogationsRequested = inscriptionJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inscription WHERE derogation_requested = true " +
                        "AND DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        Long derogationsGranted = inscriptionJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inscription WHERE derogation_granted = true " +
                        "AND DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Collect discipline distribution
        Map<String, Long> disciplineDistribution = new HashMap<>();
        List<Map<String, Object>> disciplineResults = inscriptionJdbcTemplate.queryForList(
                "SELECT discipline, COUNT(*) as count FROM inscription " +
                        "WHERE DATE(created_at) BETWEEN ? AND ? GROUP BY discipline",
                startDate, endDate);
        for (Map<String, Object> row : disciplineResults) {
            disciplineDistribution.put((String) row.get("discipline"), ((Number) row.get("count")).longValue());
        }

        // Collect laboratory distribution
        Map<String, Long> laboratoryDistribution = new HashMap<>();
        List<Map<String, Object>> labResults = inscriptionJdbcTemplate.queryForList(
                "SELECT laboratory, COUNT(*) as count FROM inscription " +
                        "WHERE DATE(created_at) BETWEEN ? AND ? GROUP BY laboratory",
                startDate, endDate);
        for (Map<String, Object> row : labResults) {
            laboratoryDistribution.put((String) row.get("laboratory"), ((Number) row.get("count")).longValue());
        }

        // Calculate average processing time
        Double averageProcessingTimeDays = inscriptionJdbcTemplate.queryForObject(
                "SELECT AVG(DATEDIFF(COALESCE(validation_date, rejection_date), created_at)) " +
                        "FROM inscription WHERE (validation_date IS NOT NULL OR rejection_date IS NOT NULL) " +
                        "AND DATE(created_at) BETWEEN ? AND ?",
                Double.class, startDate, endDate);
        if (averageProcessingTimeDays == null) {
            averageProcessingTimeDays = 0.0;
        }

        // Calculate director validation rate
        Long totalProcessed = inscriptionJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inscription WHERE (status = 'VALIDÉ' OR status = 'REJETÉ') " +
                        "AND DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        Long directorValidated = inscriptionJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inscription WHERE status = 'VALIDÉ' " +
                        "AND validated_by_director = true AND DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        Double directorValidationRate = totalProcessed > 0
                ? (directorValidated.doubleValue() / totalProcessed.doubleValue()) * 100
                : 0.0;

        // Calculate admin validation rate
        Long adminValidated = inscriptionJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inscription WHERE status = 'VALIDÉ' " +
                        "AND validated_by_admin = true AND DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        Double adminValidationRate = totalProcessed > 0
                ? (adminValidated.doubleValue() / totalProcessed.doubleValue()) * 100
                : 0.0;

        // Build DTO
        EnrollmentStatsDTO stats = EnrollmentStatsDTO.builder()
                .totalEnrollments(totalEnrollments)
                .statusDistribution(statusDistribution)
                .reinscriptionsCount(reinscriptionsCount)
                .derogationsRequested(derogationsRequested)
                .derogationsGranted(derogationsGranted)
                .disciplineDistribution(disciplineDistribution)
                .laboratoryDistribution(laboratoryDistribution)
                .averageProcessingTimeDays(averageProcessingTimeDays)
                .directorValidationRate(directorValidationRate)
                .adminValidationRate(adminValidationRate)
                .build();

        // Store in execution context
        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("enrollmentStats", stats);

        log.info("Enrollment statistics collected successfully: {} total enrollments", totalEnrollments);

        return RepeatStatus.FINISHED;
    }
}
