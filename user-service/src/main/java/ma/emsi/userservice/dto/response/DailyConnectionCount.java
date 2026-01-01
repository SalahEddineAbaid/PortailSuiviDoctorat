package ma.emsi.userservice.dto.response;

import java.time.LocalDate;

public record DailyConnectionCount(
        LocalDate date,
        long count) {
}
