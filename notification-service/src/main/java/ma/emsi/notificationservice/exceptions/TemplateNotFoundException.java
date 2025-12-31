package ma.emsi.notificationservice.exceptions;

/**
 * Exception thrown when a required email template cannot be found.
 * This exception is thrown during template loading and triggers fallback to generic template.
 */
public class TemplateNotFoundException extends NotificationServiceException {

    public TemplateNotFoundException(String templateName) {
        super("Template not found: " + templateName);
    }

    public TemplateNotFoundException(String templateName, Throwable cause) {
        super("Template not found: " + templateName, cause);
    }
}
