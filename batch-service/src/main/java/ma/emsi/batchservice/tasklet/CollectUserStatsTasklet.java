package ma.emsi.batchservice.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.dto.UserStatsDTO;
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
 * Tasklet to collect user statistics from user database for the previous month.
 * Stores the collected statistics in the job execution context for use by
 * subsequent steps.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectUserStatsTasklet implements Tasklet {

    @Qualifier("userJdbcTemplate")
    private final JdbcTemplate userJdbcTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting user statistics collection...");

        // Calculate previous month date range
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = previousMonth.atDay(1);
        LocalDate endDate = previousMonth.atEndOfMonth();

        log.info("Collecting user statistics for period: {} to {}", startDate, endDate);

        // Collect total active users
        Long totalActiveUsers = userJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE account_status = 'ACTIVE'",
                Long.class);

        // Collect role distribution
        Map<String, Long> roleDistribution = new HashMap<>();
        List<Map<String, Object>> roleResults = userJdbcTemplate.queryForList(
                "SELECT r.name, COUNT(DISTINCT ur.user_id) as count " +
                        "FROM role r " +
                        "JOIN user_roles ur ON r.id = ur.role_id " +
                        "JOIN user u ON ur.user_id = u.id " +
                        "WHERE u.account_status = 'ACTIVE' " +
                        "GROUP BY r.name");
        for (Map<String, Object> row : roleResults) {
            roleDistribution.put((String) row.get("name"), ((Number) row.get("count")).longValue());
        }

        // Collect new users count
        Long newUsersCount = userJdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user WHERE DATE(created_at) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        // Calculate connection rate
        Long usersWhoLoggedIn = userJdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT user_id) FROM user_audit " +
                        "WHERE action = 'LOGIN' AND DATE(timestamp) BETWEEN ? AND ?",
                Long.class, startDate, endDate);

        Double connectionRate = totalActiveUsers > 0
                ? (usersWhoLoggedIn.doubleValue() / totalActiveUsers.doubleValue()) * 100
                : 0.0;

        // Build DTO
        UserStatsDTO stats = UserStatsDTO.builder()
                .totalActiveUsers(totalActiveUsers)
                .roleDistribution(roleDistribution)
                .newUsersCount(newUsersCount)
                .connectionRate(connectionRate)
                .build();

        // Store in execution context
        chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .put("userStats", stats);

        log.info("User statistics collected successfully: {} active users, {} new users",
                totalActiveUsers, newUsersCount);

        return RepeatStatus.FINISHED;
    }
}
