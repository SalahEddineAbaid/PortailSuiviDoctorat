package ma.emsi.userservice.dto.response;

import java.util.Map;

public record UserStatisticsResponse(
        long total,
        Map<String, Long> byRole,
        long active,
        long disabled,
        long locked,
        long newThisMonth) {
}
