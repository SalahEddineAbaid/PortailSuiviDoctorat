package ma.emsi.defenseservice.exception;

public class AuthorizationAlreadyExistsException extends RuntimeException {
    public AuthorizationAlreadyExistsException(String message) {
        super(message);
    }
}
