package ma.emsi.userservice.enums;

/**
 * Enum representing the status of a user account.
 * Used to control account access and enforce security policies.
 */
public enum AccountStatus {
    /**
     * Account is active and can be used normally
     */
    ACTIVE,

    /**
     * Account has been disabled by an administrator
     */
    DISABLED,

    /**
     * Account is temporarily locked due to failed login attempts
     */
    LOCKED
}
