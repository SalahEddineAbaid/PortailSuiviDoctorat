# Property-Based Testing Generators

This package contains custom generators for property-based testing using JUnit QuickCheck.

## Overview

Property-based testing verifies that properties hold true across a wide range of randomly generated inputs. These generators create test data for the inscription-service domain entities.

## Available Generators

### 1. InscriptionGenerator

Generates random `Inscription` entities with:
- Random IDs, doctorant IDs, and directeur IDs
- All possible inscription types and statuses
- Random dates (including edge cases)
- Random derogation and blocking flags
- Random comments and thesis subjects

**Usage:**
```java
@Property(trials = 100)
public void someProperty(@ForAll @From(InscriptionGenerator.class) Inscription inscription) {
    // Test your property
}
```

### 2. DocumentGenerator

Generates random `MultipartFile` documents with:
- Mix of valid MIME types (PDF, JPEG, PNG) and invalid types
- Various file sizes including edge cases:
  - Very small files (< 1KB)
  - Normal files (1KB - 5MB)
  - Large files (5MB - 9.9MB)
  - Exactly 10MB (boundary case)
  - 10MB + 1 byte (boundary case)
  - Oversized files (> 10MB)

**Usage:**
```java
@Property(trials = 100)
public void documentValidation(@ForAll @From(DocumentGenerator.class) MultipartFile file) {
    // Test document validation
}
```

### 3. InvalidMimeTypeGenerator

Generates only invalid MIME types for testing validation rejection:
- Office documents (Word, Excel, PowerPoint)
- Text formats (plain text, HTML, JSON, XML)
- Archives (ZIP, RAR, 7z)
- Executables
- Media files (video, audio)
- Malformed MIME types

**Usage:**
```java
@Property(trials = 100)
public void rejectsInvalidMimeTypes(@ForAll @From(InvalidMimeTypeGenerator.class) String mimeType) {
    // Test that invalid MIME types are rejected
}
```

### 4. DerogationRequestGenerator

Generates random `DerogationRequest` entities with:
- Various derogation statuses
- Random motifs and validation comments
- Random dates and validator IDs
- Optional justification documents
- Proper length constraints

**Usage:**
```java
@Property(trials = 100)
public void derogationWorkflow(@ForAll @From(DerogationRequestGenerator.class) DerogationRequest derogation) {
    // Test derogation workflow
}
```

### 5. CampagneGenerator

Generates random `Campagne` entities with:
- Various campaign types (INSCRIPTION, REINSCRIPTION)
- Different duration scenarios:
  - Normal campaigns (2-3 months)
  - Short campaigns (1 week - 1 month)
  - Long campaigns (6+ months)
  - Past, current, and future campaigns
  - Same-day campaigns (edge case)
  - Invalid campaigns (dateFin before dateDebut)

**Usage:**
```java
@Property(trials = 100)
public void campaignManagement(@ForAll @From(CampagneGenerator.class) Campagne campagne) {
    // Test campaign logic
}
```

## Configuration

All generators are configured to run with a minimum of 100 trials as specified in the design document. You can override this in individual tests:

```java
@Property(trials = 200) // Run 200 iterations instead of default
public void myProperty(@ForAll @From(InscriptionGenerator.class) Inscription inscription) {
    // Test code
}
```

## Edge Cases

The generators are designed to produce edge cases to catch boundary conditions:

- **InscriptionGenerator**: Null dates, various statuses, derogation flags
- **DocumentGenerator**: Boundary file sizes (exactly 10MB, 10MB + 1 byte)
- **InvalidMimeTypeGenerator**: Malformed MIME types, case variations
- **DerogationRequestGenerator**: Null validators, various approval states
- **CampagneGenerator**: Invalid date ranges, same-day campaigns

## Best Practices

1. **Tag your tests**: Always include a comment referencing the correctness property:
   ```java
   /**
    * Feature: inscription-service-finalisation, Property 1: MIME type validation
    * Validates: Requirements 1.1
    */
   @Property(trials = 100)
   public void mimeTypeValidation(...) { }
   ```

2. **Filter invalid inputs**: Use assumptions to filter out inputs that are outside the domain:
   ```java
   @Property(trials = 100)
   public void someProperty(@ForAll @From(InscriptionGenerator.class) Inscription inscription) {
       assumeThat(inscription.getDatePremiereInscription(), notNullValue());
       // Test code
   }
   ```

3. **Combine generators**: You can use multiple generators in one test:
   ```java
   @Property(trials = 100)
   public void complexProperty(
       @ForAll @From(InscriptionGenerator.class) Inscription inscription,
       @ForAll @From(CampagneGenerator.class) Campagne campagne) {
       // Test code
   }
   ```

## Dependencies

These generators require:
- JUnit QuickCheck Core (com.pholser:junit-quickcheck-core:1.0)
- JUnit QuickCheck Generators (com.pholser:junit-quickcheck-generators:1.0)
- Spring Test (for MockMultipartFile)

All dependencies are already configured in the project's pom.xml.
