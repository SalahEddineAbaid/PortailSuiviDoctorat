package ma.emsi.userservice.dto.event;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Event record for user-related events published to Kafka.
 * Used for inter-service communication to notify other services of user
 * changes.
 */
public record UserEvent(
        String eventType,
        Long userId,
        String email,
        LocalDateTime timestamp,
        Map<String, Object> metadata) {
    /**
     * Creates a UserEvent with the current timestamp.
     */
    public static UserEvent of(String eventType, Long userId, String email, Map<String, Object> metadata) {
        return new UserEvent(eventType, userId, email, LocalDateTime.now(), metadata);
    }

    /**
     * Creates a UserEvent with the current timestamp and no metadata.
     */
    public static UserEvent of(String eventType, Long userId, String email) {
        return new UserEvent(eventType, userId, email, LocalDateTime.now(), Map.of());
    }
}
