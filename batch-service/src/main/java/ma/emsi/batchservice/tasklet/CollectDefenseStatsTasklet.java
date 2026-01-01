package ma.emsi.batchservice.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.dto.DefenseStatsDTO;
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
 * Tasklet to collect defense statistics from defense database for the previous
 * month.
 * Stores the collected statistics in the job execution context for use by
 * subsequent steps.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectDefenseStatsTasklet implements Tasklet {

    @Qualifier("defenseJdbcTemplate")
    private final JdbcTemplate defenseJdbcTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting defense statistics collection...");

        // Calculate previous month date range
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = previousMonth.atDay(1);
        LocalDate endDate = previousMonth.atEndOfMonth();

        log.info("Collecting defense statistics for period: {} to {}", startDate, endDate);

        // Collect defense requests count
        Long defenseRequestsCount = defenseJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM defense_request WHERE DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Collect completed defenses count
        Long completedDefensesCount = defenseJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM defense WHERE status = 'COMPLETED' " +
                        "AND DATE(defense_date) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Collect mention distribution
        Map<String, Long> mentionDistribution = new HashMap<>();
        List<Map<String, Object>> mentionResults = defenseJdbcTemplate.queryForList(
                "SELECT mention, COUNT(*) as count FROM defense " +
                        "WHERE status = 'COMPLETED' AND DATE(defense_date) BETWEEN ? AND ? " +
                        "GROUP BY mention",
                startDate, endDate);
        for (Map<String, Object> row : mentionResults) {
            mentionDistribution.put((String) row.get("mention"), ((Number) row.get("count")).longValue());
        }

        // Collect jury count
        Long juryCount = defenseJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jury WHERE DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Collect submitted reports count
        Long submittedReportsCount = defenseJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rapport WHERE DATE(submitted_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Calculate jury member acceptance rate
        Long totalInvitations = defenseJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jury_member WHERE DATE(invited_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        Long acceptedInvitations = defenseJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jury_member WHERE status = 'ACCEPTED' " +
                        "AND DATE(invited_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        Double juryMemberAcceptanceRate = totalInvitations > 0
                ? (acceptedInvitations.doubleValue() / totalInvitations.doubleValue()) * 100
                : 0.0;

        // Calculate average time from request to authorization
        Double averageTimeRequestToAuthorizationDays = defenseJdbcTemplate.queryForObject(
                "SELECT AVG(DATEDIFF(a.authorization_date, dr.created_at)) " +
                        "FROM defense_request dr " +
                        "JOIN autorisation_soutenance a ON dr.id = a.defense_request_id " +
                        "WHERE a.statut = 'ACCORDÉE' AND DATE(dr.created_at) BETWEEN ? AND ?",
                Double.class, startDate, endDate);
        if (averageTimeRequestToAuthorizationDays == null) {
            averageTimeRequestToAuthorizationDays = 0.0;
        }

        // Calculate average time from authorization to defense
        Double averageTimeAuthorizationToDefenseDays = defenseJdbcTemplate.queryForObject(
                "SELECT AVG(DATEDIFF(d.defense_date, a.authorization_date)) " +
                        "FROM defense d " +
                        "JOIN autorisation_soutenance a ON d.defense_request_id = a.defense_request_id " +
                        "WHERE d.status = 'COMPLETED' AND DATE(d.defense_date) BETWEEN ? AND ?",
                Double.class, startDate, endDate);
        if (averageTimeAuthorizationToDefenseDays == null) {
            averageTimeAuthorizationToDefenseDays = 0.0;
        }

        // Build DTO
        DefenseStatsDTO stats = DefenseStatsDTO.builder()
                .defenseRequestsCount(defenseRequestsCount)
                .completedDefensesCount(completedDefensesCount)
                .mentionDistribution(mentionDistribution)
                .juryCount(juryCount)
                .submittedReportsCount(submittedReportsCount)
                .juryMemberAcceptanceRate(juryMemberAcceptanceRate)
                .averageTimeRequestToAuthorizationDays(averageTimeRequestToAuthorizationDays)
                .averageTimeAuthorizationToDefenseDays(averageTimeAuthorizationToDefenseDays)
                .build();

        // Store in execution context
        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("defenseStats", stats);

        log.info("Defense statistics collected successfully: {} defense requests, {} completed",
                defenseRequestsCount, completedDefensesCount);

        return RepeatStatus.FINISHED;
    }
}
