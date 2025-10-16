package ma.emsi.userservice.dto.response;

import java.time.Instant;

public record ErrorResponse(
        String message,
        int status,
        Instant timestamp
) {}
