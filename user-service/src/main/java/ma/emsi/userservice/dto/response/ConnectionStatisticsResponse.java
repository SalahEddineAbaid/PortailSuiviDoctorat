package ma.emsi.userservice.dto.response;

import java.util.List;
import java.util.Map;

public record ConnectionStatisticsResponse(
        List<DailyConnectionCount> dailyCounts,
        Map<String, Long> byRole) {
}
