package ma.emsi.notificationservice.enums;

/**
 * Enum representing the status of a notification in its lifecycle.
 */
public enum StatutNotification {
    /**
     * Notification has been received but not yet processed
     */
    PENDING,
    
    /**
     * Notification has been successfully sent
     */
    SENT,
    
    /**
     * Notification failed to send after all retry attempts
     */
    FAILED,
    
    /**
     * Notification is currently being retried after a failure
     */
    RETRYING
}
