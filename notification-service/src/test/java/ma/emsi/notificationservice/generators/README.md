# Property-Based Testing Generators

This package contains custom generators for property-based testing using JUnit QuickCheck.
These generators produce random test data to validate the notification service's correctness properties.

## Generators

### 1. NotificationDTOGenerator

**Purpose**: Generates random valid `NotificationDTO` objects for testing notification processing logic.

**Features**:
- Generates valid email addresses from predefined domains (@emsi.ma, @portail-doctorat.ma, etc.)
- Covers all `TypeNotification` enum values (21 notification types)
- Covers all `PrioriteNotification` enum values (NORMALE, HAUTE, URGENTE)
- Generates realistic French subjects and messages
- Produces random template variable maps with common keys
- **Edge cases**: 
  - Sometimes generates `null` messageTexte (50% probability)
  - Sometimes generates empty `donnees` maps (20% probability)

**Usage**:
```java
@Property(trials = 100)
public void testNotificationProcessing(
    @From(NotificationDTOGenerator.class) NotificationDTO notification) {
    // Test logic here
}
```

### 2. InvalidEmailGenerator

**Purpose**: Generates invalid email addresses to test email validation logic.

**Features**:
- Missing @ symbol
- Missing domain or local part
- Multiple @ symbols
- Invalid characters (spaces, special chars)
- Empty or whitespace strings
- Leading/trailing/consecutive dots
- Invalid domain formats
- Unicode characters
- Very long strings (300+ characters)
- Null-like strings ("null", "undefined")

**Edge cases covered**: 60+ different invalid email patterns

**Usage**:
```java
@Property(trials = 100)
public void testEmailValidation(
    @From(InvalidEmailGenerator.class) String invalidEmail) {
    // Should fail validation
}
```

### 3. TemplateVariablesGenerator

**Purpose**: Generates random maps of template variables for testing email template interpolation.

**Features**:
- Person-related variables (nomDoctorant, prenomDoctorant, emailDoctorant, etc.)
- Document-related variables (titreSujet, numeroInscription, etc.)
- Date-related variables (dateInscription, dateSoutenance, etc.)
- Location-related variables (lieuSoutenance, salle, etc.)
- Academic-related variables (specialite, laboratoire, etc.)
- Link-related variables (lienPortail, lienDocument, etc.)
- Generates contextually appropriate values based on key names
- **Edge cases**:
  - 10% chance of generating empty maps
  - Variable number of entries (1-15)

**Usage**:
```java
@Property(trials = 100)
public void testTemplateInterpolation(
    @From(TemplateVariablesGenerator.class) Map<String, Object> variables) {
    // Test template rendering
}
```

### 4. TypeNotificationGenerator

**Purpose**: Generates random `TypeNotification` enum values.

**Features**:
- Covers all 21 notification types:
  - Inscription workflow (7 types)
  - Defense workflow (8 types)
  - System events (6 types)
- Uniform distribution across all types

**Usage**:
```java
@Property(trials = 100)
public void testNotificationType(
    @From(TypeNotificationGenerator.class) TypeNotification type) {
    // Test type-specific logic
}
```

## Validation Tests

The `GeneratorsTest` class validates that all generators produce correct data:

1. **NotificationDTOGenerator validation**:
   - All required fields are non-null
   - Email addresses match valid format
   - Subjects are non-empty
   - Types are valid enum values

2. **InvalidEmailGenerator validation**:
   - Generated emails fail standard validation
   - Covers various invalid patterns

3. **TemplateVariablesGenerator validation**:
   - Maps contain non-null keys and values
   - Keys are non-empty strings

4. **TypeNotificationGenerator validation**:
   - All generated types are valid enum values
   - Statistical coverage of all types

## Requirements Coverage

These generators support testing the following requirements:

- **14.1**: Unit test coverage (generators enable comprehensive testing)
- **14.2**: Kafka consumer testing (NotificationDTOGenerator)
- **14.3**: Email sending tests (InvalidEmailGenerator for validation)
- **14.4**: Persistence testing (NotificationDTOGenerator)
- **14.5**: Resilience testing (generators for failure scenarios)
- **14.6**: DLQ testing (NotificationDTOGenerator)
- **14.7**: REST endpoint testing (all generators)

## Running Generator Tests

To verify generators work correctly:

```bash
./mvnw test -Dtest=GeneratorsTest
```

This runs 100 trials for each property to ensure generators produce valid data.

## Best Practices

1. **Use appropriate generators**: Match the generator to what you're testing
2. **Configure trial count**: Use at least 100 trials for good coverage
3. **Test edge cases**: Generators include edge cases automatically
4. **Combine generators**: Use multiple generators in a single test when needed
5. **Validate assumptions**: Always verify generator output matches expectations

## Example Property-Based Test

```java
@RunWith(JUnitQuickcheck.class)
public class NotificationProcessingPropertyTest {
    
    @Property(trials = 100)
    public void validNotificationsShouldBeProcessed(
            @From(NotificationDTOGenerator.class) NotificationDTO notification) {
        
        // Given: A valid notification
        assertNotNull(notification.getType());
        assertNotNull(notification.getDestinataire());
        
        // When: Processing the notification
        NotificationResult result = service.process(notification);
        
        // Then: Should succeed
        assertTrue(result.isSuccess());
    }
    
    @Property(trials = 100)
    public void invalidEmailsShouldBeRejected(
            @From(InvalidEmailGenerator.class) String invalidEmail) {
        
        // Given: An invalid email
        NotificationDTO notification = NotificationDTO.builder()
            .destinataire(invalidEmail)
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .sujet("Test")
            .build();
        
        // When: Validating the notification
        ValidationResult result = validator.validate(notification);
        
        // Then: Should fail validation
        assertFalse(result.isValid());
    }
}
```
