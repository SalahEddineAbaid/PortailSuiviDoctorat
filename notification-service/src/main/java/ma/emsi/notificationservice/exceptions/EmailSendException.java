package ma.emsi.notificationservice.exceptions;

/**
 * Exception thrown when an email fails to send.
 * This exception wraps underlying email sending failures and may trigger retries
 * depending on the underlying cause.
 */
public class EmailSendException extends NotificationServiceException {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
