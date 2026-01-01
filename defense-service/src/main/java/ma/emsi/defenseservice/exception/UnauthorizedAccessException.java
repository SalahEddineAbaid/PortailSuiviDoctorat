package ma.emsi.defenseservice.exception;

/**
 * Exception thrown when a user attempts to access a resource they don't have
 * permission for
 * Implements Requirements 3.7
 */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
