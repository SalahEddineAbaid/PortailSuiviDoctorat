package ma.emsi.notificationservice.exceptions;

/**
 * Exception thrown when an invalid or unrecognized notification type is provided.
 * This exception is thrown during type validation and should not trigger retries.
 */
public class InvalidNotificationTypeException extends NotificationServiceException {

    public InvalidNotificationTypeException(String type) {
        super("Invalid notification type: " + type);
    }

    public InvalidNotificationTypeException(String type, Throwable cause) {
        super("Invalid notification type: " + type, cause);
    }
}
