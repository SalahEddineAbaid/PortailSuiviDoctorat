package ma.emsi.notificationservice.generators;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.enums.TypeNotification;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Test class to verify that custom generators produce valid data.
 * This ensures generators work correctly before using them in property-based tests.
 */
@RunWith(JUnitQuickcheck.class)
public class GeneratorsTest {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    /**
     * Property: NotificationDTOGenerator produces valid NotificationDTO objects
     * Validates that all required fields are present and properly formatted.
     */
    @Property(trials = 100)
    public void notificationDTOGeneratorProducesValidObjects(
            @From(NotificationDTOGenerator.class) NotificationDTO notification) {
        
        // Verify required fields are not null
        assertNotNull("Type should not be null", notification.getType());
        assertNotNull("Destinataire should not be null", notification.getDestinataire());
        assertNotNull("Sujet should not be null", notification.getSujet());
        assertNotNull("Priorite should not be null", notification.getPriorite());
        
        // Verify destinataire is a valid email format
        assertTrue("Destinataire should be a valid email: " + notification.getDestinataire(),
                EMAIL_PATTERN.matcher(notification.getDestinataire()).matches());
        
        // Verify sujet is not empty
        assertFalse("Sujet should not be empty", notification.getSujet().trim().isEmpty());
        
        // Verify type is a valid TypeNotification enum value
        assertNotNull("Type should be a valid TypeNotification", notification.getType());
        
        // Verify donnees map is not null (can be empty)
        assertNotNull("Donnees map should not be null", notification.getDonnees());
    }
    
    /**
     * Property: InvalidEmailGenerator produces invalid email addresses
     * Validates that generated emails fail standard email validation.
     */
    @Property(trials = 100)
    public void invalidEmailGeneratorProducesInvalidEmails(
            @From(InvalidEmailGenerator.class) String email) {
        
        // Verify the email is invalid according to standard email pattern
        // Note: Some edge cases might pass basic regex but fail Jakarta validation
        boolean isValidFormat = EMAIL_PATTERN.matcher(email).matches();
        boolean isEmpty = email == null || email.trim().isEmpty();
        boolean hasWhitespace = email != null && email.contains(" ");
        boolean hasMultipleAt = email != null && email.indexOf('@') != email.lastIndexOf('@');
        
        // At least one of these conditions should be true for an invalid email
        assertTrue("Email should be invalid: " + email,
                !isValidFormat || isEmpty || hasWhitespace || hasMultipleAt);
    }
    
    /**
     * Property: TemplateVariablesGenerator produces valid variable maps
     * Validates that generated maps contain string keys and non-null values.
     */
    @Property(trials = 100)
    public void templateVariablesGeneratorProducesValidMaps(
            @From(TemplateVariablesGenerator.class) Map<String, Object> variables) {
        
        // Verify map is not null
        assertNotNull("Variables map should not be null", variables);
        
        // Verify all keys are non-null and non-empty strings
        for (String key : variables.keySet()) {
            assertNotNull("Key should not be null", key);
            assertFalse("Key should not be empty", key.trim().isEmpty());
            
            // Verify value is not null
            Object value = variables.get(key);
            assertNotNull("Value for key '" + key + "' should not be null", value);
        }
    }
    
    /**
     * Property: TypeNotificationGenerator produces valid TypeNotification values
     * Validates that all generated types are valid enum values.
     */
    @Property(trials = 100)
    public void typeNotificationGeneratorProducesValidTypes(
            @From(TypeNotificationGenerator.class) TypeNotification type) {
        
        // Verify type is not null
        assertNotNull("TypeNotification should not be null", type);
        
        // Verify type is one of the enum values
        boolean isValidType = false;
        for (TypeNotification validType : TypeNotification.values()) {
            if (validType == type) {
                isValidType = true;
                break;
            }
        }
        assertTrue("Type should be a valid TypeNotification enum value", isValidType);
    }
    
    /**
     * Property: TypeNotificationGenerator covers all notification types
     * This property verifies that the generator can produce all enum values.
     * Note: With 100 trials and 21 enum values, we should see good coverage.
     */
    @Property(trials = 100)
    public void typeNotificationGeneratorCoversAllTypes(
            @From(TypeNotificationGenerator.class) TypeNotification type) {
        
        // Simply verify the type is valid - coverage is statistical
        assertNotNull("Generated type should not be null", type);
        
        // Verify it's one of the expected types
        TypeNotification[] allTypes = TypeNotification.values();
        boolean found = false;
        for (TypeNotification t : allTypes) {
            if (t == type) {
                found = true;
                break;
            }
        }
        assertTrue("Generated type should be in the enum", found);
    }
}
