package ma.emsi.userservice.enums;

/**
 * Enum representing audit action types for security-sensitive operations.
 * Used to track and log all important user actions in the system.
 */
public enum AuditAction {
    /**
     * Successful user login
     */
    LOGIN,

    /**
     * Failed login attempt
     */
    LOGIN_FAILED,

    /**
     * User logout
     */
    LOGOUT,

    /**
     * User password change
     */
    PASSWORD_CHANGE,

    /**
     * Role assigned to user
     */
    ROLE_ASSIGNED,

    /**
     * Role removed from user
     */
    ROLE_REMOVED,

    /**
     * User profile modified
     */
    PROFILE_MODIFIED,

    /**
     * Account disabled by administrator
     */
    ACCOUNT_DISABLED,

    /**
     * Account enabled by administrator
     */
    ACCOUNT_ENABLED,

    /**
     * Account locked due to failed login attempts
     */
    ACCOUNT_LOCKED,

    /**
     * Account unlocked (automatically or by administrator)
     */
    ACCOUNT_UNLOCKED
}
