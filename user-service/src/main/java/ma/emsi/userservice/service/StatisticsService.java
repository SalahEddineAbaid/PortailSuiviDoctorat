package ma.emsi.userservice.service;

import lombok.RequiredArgsConstructor;
import ma.emsi.userservice.dto.response.ConnectionStatisticsResponse;
import ma.emsi.userservice.dto.response.DailyConnectionCount;
import ma.emsi.userservice.dto.response.UserStatisticsResponse;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.entity.UserAudit;
import ma.emsi.userservice.enums.AccountStatus;
import ma.emsi.userservice.enums.AuditAction;
import ma.emsi.userservice.repository.UserAuditRepository;
import ma.emsi.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final UserRepository userRepository;
    private final UserAuditRepository auditRepository;

    public UserStatisticsResponse getUserStatistics() {
        // Count total users
        long total = userRepository.count();

        // Count by role
        List<User> allUsers = userRepository.findAll();
        Map<String, Long> byRole = allUsers.stream()
                .flatMap(user -> user.getRoles().stream())
                .map(role -> role.getName().name())
                .collect(Collectors.groupingBy(
                        roleName -> roleName,
                        Collectors.counting()));

        // Count by account status
        long active = userRepository.countByAccountStatus(AccountStatus.ACTIVE);
        long disabled = userRepository.countByAccountStatus(AccountStatus.DISABLED);
        long locked = userRepository.countByAccountStatus(AccountStatus.LOCKED);

        // Count new users this month
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        long newThisMonth = userRepository.countByCreatedAtAfter(startOfMonth);

        return new UserStatisticsResponse(
                total,
                byRole,
                active,
                disabled,
                locked,
                newThisMonth);
    }

    public ConnectionStatisticsResponse getConnectionStatistics() {
        // Query LOGIN audits for last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<UserAudit> loginAudits = auditRepository.findByActionAndTimestampBetween(
                AuditAction.LOGIN,
                thirtyDaysAgo,
                LocalDateTime.now());

        // Group by date
        Map<LocalDate, Long> countsByDate = loginAudits.stream()
                .collect(Collectors.groupingBy(
                        audit -> audit.getTimestamp().toLocalDate(),
                        Collectors.counting()));

        List<DailyConnectionCount> dailyCounts = countsByDate.entrySet().stream()
                .map(entry -> new DailyConnectionCount(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> a.date().compareTo(b.date()))
                .collect(Collectors.toList());

        // Group by role
        Map<String, Long> byRole = new HashMap<>();
        for (UserAudit audit : loginAudits) {
            User user = userRepository.findById(audit.getUserId()).orElse(null);
            if (user != null) {
                for (Role role : user.getRoles()) {
                    String roleName = role.getName().name();
                    byRole.put(roleName, byRole.getOrDefault(roleName, 0L) + 1);
                }
            }
        }

        return new ConnectionStatisticsResponse(dailyCounts, byRole);
    }
}
