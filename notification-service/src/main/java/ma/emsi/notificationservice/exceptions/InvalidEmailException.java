package ma.emsi.notificationservice.exceptions;

/**
 * Exception thrown when an invalid email address is provided.
 * This exception is thrown during email validation and should not trigger retries.
 */
public class InvalidEmailException extends NotificationServiceException {

    public InvalidEmailException(String email) {
        super("Invalid email format: " + email);
    }

    public InvalidEmailException(String email, Throwable cause) {
        super("Invalid email format: " + email, cause);
    }
}
