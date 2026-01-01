package ma.emsi.userservice.exception;

import org.springframework.security.core.AuthenticationException;

public class AccountLockedException extends AuthenticationException {
    public AccountLockedException(String message) {
        super(message);
    }
}
