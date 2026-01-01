package ma.emsi.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.enums.AccountStatus;
import ma.emsi.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service for managing login attempts and account locking.
 * Implements failed login protection by tracking attempts and locking accounts
 * after exceeding the maximum allowed failures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${user-service.account.max-failed-logins:5}")
    private int maxFailedLogins;

    @Value("${user-service.account.lockout-duration:30m}")
    private String lockoutDurationString;

    /**
     * Handle successful login by resetting failed login attempts and clearing
     * lockout.
     *
     * @param user the user who successfully logged in
     */
    @Transactional
    public void loginSucceeded(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getLockoutExpiration() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockoutExpiration(null);

            // If account was locked, set it back to active
            if (user.getAccountStatus() == AccountStatus.LOCKED) {
                user.setAccountStatus(AccountStatus.ACTIVE);
                log.info("Account unlocked for user {} after successful login", user.getId());
            }

            userRepository.save(user);
            log.info("Reset failed login attempts for user {}", user.getId());
        }
    }

    /**
     * Handle failed login by incrementing counter and locking account if max
     * attempts reached.
     *
     * @param user      the user who failed to login
     * @param ipAddress the IP address of the failed attempt
     */
    @Transactional
    public void loginFailed(User user, String ipAddress) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        log.warn("Failed login attempt {} for user {} from IP {}",
                attempts, user.getId(), ipAddress);

        // Lock account if max attempts reached
        if (attempts >= maxFailedLogins) {
            Duration lockoutDuration = parseDuration(lockoutDurationString);
            LocalDateTime lockoutExpiration = LocalDateTime.now().plus(lockoutDuration);

            user.setAccountStatus(AccountStatus.LOCKED);
            user.setLockoutExpiration(lockoutExpiration);

            userRepository.save(user);

            auditService.logAccountLocked(user.getId(), ipAddress, attempts);

            log.warn("Account locked for user {} after {} failed attempts. Lockout expires at {}",
                    user.getId(), attempts, lockoutExpiration);
        } else {
            userRepository.save(user);
        }
    }

    /**
     * Check if an account is currently locked.
     * Automatically unlocks the account if the lockout period has expired.
     *
     * @param user the user to check
     * @return true if account is locked, false otherwise
     */
    @Transactional
    public boolean isAccountLocked(User user) {
        // If account status is not LOCKED, it's not locked
        if (user.getAccountStatus() != AccountStatus.LOCKED) {
            return false;
        }

        // Check if lockout has expired
        LocalDateTime lockoutExpiration = user.getLockoutExpiration();
        if (lockoutExpiration == null || LocalDateTime.now().isAfter(lockoutExpiration)) {
            // Auto-unlock the account
            user.setAccountStatus(AccountStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setLockoutExpiration(null);
            userRepository.save(user);

            log.info("Account automatically unlocked for user {} after lockout expiration",
                    user.getId());

            return false;
        }

        // Account is still locked
        return true;
    }

    /**
     * Parse duration string (e.g., "30m", "1h", "2d") into Duration object.
     *
     * @param durationString the duration string to parse
     * @return Duration object
     */
    private Duration parseDuration(String durationString) {
        if (durationString == null || durationString.isEmpty()) {
            return Duration.ofMinutes(30); // Default to 30 minutes
        }

        try {
            String value = durationString.substring(0, durationString.length() - 1);
            char unit = durationString.charAt(durationString.length() - 1);
            long amount = Long.parseLong(value);

            return switch (unit) {
                case 's' -> Duration.ofSeconds(amount);
                case 'm' -> Duration.ofMinutes(amount);
                case 'h' -> Duration.ofHours(amount);
                case 'd' -> Duration.ofDays(amount);
                default -> Duration.ofMinutes(30); // Default fallback
            };
        } catch (Exception e) {
            log.warn("Failed to parse lockout duration '{}', using default 30 minutes",
                    durationString, e);
            return Duration.ofMinutes(30);
        }
    }
}
