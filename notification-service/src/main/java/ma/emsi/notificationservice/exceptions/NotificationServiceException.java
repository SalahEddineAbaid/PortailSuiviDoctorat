package ma.emsi.notificationservice.exceptions;

/**
 * Base exception class for all notification service exceptions.
 * This provides a common parent for all custom exceptions in the notification service.
 */
public class NotificationServiceException extends RuntimeException {

    public NotificationServiceException(String message) {
        super(message);
    }

    public NotificationServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
