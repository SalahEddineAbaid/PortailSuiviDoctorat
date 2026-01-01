package ma.emsi.userservice.service;

import ma.emsi.userservice.dto.event.UserEvent;
import ma.emsi.userservice.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for publishing user-related events to Kafka.
 * Implements non-blocking event publishing with error handling.
 * If event publishing fails, the error is logged but the primary operation
 * continues.
 */
@Service
public class UserEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    @Value("${spring.kafka.topic.user-events:user-events}")
    private String userEventsTopic;

    public UserEventPublisher(KafkaTemplate<String, UserEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a USER_REGISTERED event when a new user registers.
     * 
     * @param user The newly registered user
     */
    public void publishUserRegistered(User user) {
        try {
            UserEvent event = UserEvent.of(
                    "USER_REGISTERED",
                    user.getId(),
                    user.getEmail(),
                    Map.of(
                            "firstName", user.getFirstName(),
                            "lastName", user.getLastName(),
                            "roles", user.getRoles().stream()
                                    .map(role -> role.getName().name())
                                    .toList()));

            kafkaTemplate.send(userEventsTopic, user.getId().toString(), event);
            logger.info("Published USER_REGISTERED event for user: {}", user.getEmail());

        } catch (Exception e) {
            logger.error("Failed to publish USER_REGISTERED event for user: {}. Error: {}",
                    user.getEmail(), e.getMessage(), e);
            // Non-blocking: do not throw exception
        }
    }

    /**
     * Publishes a USER_DISABLED event when an admin disables a user account.
     * 
     * @param userId The ID of the disabled user
     * @param email  The email of the disabled user
     * @param reason The reason for disabling the account
     */
    public void publishUserDisabled(Long userId, String email, String reason) {
        try {
            UserEvent event = UserEvent.of(
                    "USER_DISABLED",
                    userId,
                    email,
                    Map.of("reason", reason));

            kafkaTemplate.send(userEventsTopic, userId.toString(), event);
            logger.info("Published USER_DISABLED event for user: {}", email);

        } catch (Exception e) {
            logger.error("Failed to publish USER_DISABLED event for user: {}. Error: {}",
                    email, e.getMessage(), e);
            // Non-blocking: do not throw exception
        }
    }

    /**
     * Publishes a USER_ENABLED event when an admin enables a previously disabled
     * account.
     * 
     * @param userId The ID of the enabled user
     * @param email  The email of the enabled user
     */
    public void publishUserEnabled(Long userId, String email) {
        try {
            UserEvent event = UserEvent.of(
                    "USER_ENABLED",
                    userId,
                    email);

            kafkaTemplate.send(userEventsTopic, userId.toString(), event);
            logger.info("Published USER_ENABLED event for user: {}", email);

        } catch (Exception e) {
            logger.error("Failed to publish USER_ENABLED event for user: {}. Error: {}",
                    email, e.getMessage(), e);
            // Non-blocking: do not throw exception
        }
    }

    /**
     * Publishes a PASSWORD_CHANGED event when a user changes their password.
     * 
     * @param userId The ID of the user who changed their password
     * @param email  The email of the user
     */
    public void publishPasswordChanged(Long userId, String email) {
        try {
            UserEvent event = UserEvent.of(
                    "PASSWORD_CHANGED",
                    userId,
                    email);

            kafkaTemplate.send(userEventsTopic, userId.toString(), event);
            logger.info("Published PASSWORD_CHANGED event for user: {}", email);

        } catch (Exception e) {
            logger.error("Failed to publish PASSWORD_CHANGED event for user: {}. Error: {}",
                    email, e.getMessage(), e);
            // Non-blocking: do not throw exception
        }
    }

    /**
     * Publishes a ROLE_ASSIGNED event when a role is assigned to a user.
     * 
     * @param userId   The ID of the user
     * @param email    The email of the user
     * @param roleName The name of the assigned role
     */
    public void publishRoleAssigned(Long userId, String email, String roleName) {
        try {
            UserEvent event = UserEvent.of(
                    "ROLE_ASSIGNED",
                    userId,
                    email,
                    Map.of("role", roleName));

            kafkaTemplate.send(userEventsTopic, userId.toString(), event);
            logger.info("Published ROLE_ASSIGNED event for user: {} with role: {}", email, roleName);

        } catch (Exception e) {
            logger.error("Failed to publish ROLE_ASSIGNED event for user: {}. Error: {}",
                    email, e.getMessage(), e);
            // Non-blocking: do not throw exception
        }
    }

    /**
     * Publishes a PROFILE_COMPLETED event when a user completes their profile.
     * 
     * @param userId The ID of the user who completed their profile
     */
    public void publishProfileCompleted(Long userId) {
        try {
            UserEvent event = UserEvent.of(
                    "PROFILE_COMPLETED",
                    userId,
                    null);

            kafkaTemplate.send(userEventsTopic, userId.toString(), event);
            logger.info("Published PROFILE_COMPLETED event for user ID: {}", userId);

        } catch (Exception e) {
            logger.error("Failed to publish PROFILE_COMPLETED event for user ID: {}. Error: {}",
                    userId, e.getMessage(), e);
            // Non-blocking: do not throw exception
        }
    }
}
