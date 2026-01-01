package ma.emsi.userservice.exception;

public class SelfDisableException extends RuntimeException {
    public SelfDisableException(String message) {
        super(message);
    }
}
