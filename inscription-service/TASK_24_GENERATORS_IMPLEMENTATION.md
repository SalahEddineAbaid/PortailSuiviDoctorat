# Task 24: Custom Generators for Property-Based Tests - Implementation Summary

## Overview

This document summarizes the implementation of custom generators for property-based testing using JUnit QuickCheck. These generators will be used to test the correctness properties defined in the design document.

## Implemented Generators

### 1. InscriptionGenerator
**Location:** `src/test/java/ma/emsi/inscriptionservice/generators/InscriptionGenerator.java`

**Purpose:** Generates random `Inscription` entities for property-based testing.

**Features:**
- Generates random IDs for doctorant, directeur, and inscription
- Covers all `TypeInscription` values (PREMIERE_INSCRIPTION, REINSCRIPTION)
- Covers all `StatutInscription` values (BROUILLON, SOUMIS, EN_ATTENTE_DIRECTEUR, etc.)
- Generates random dates between 2018-2025
- Includes optional fields (comments, validation dates)
- Generates random derogation and blocking flags
- Creates minimal associated Campagne entity

**Edge Cases:**
- Null validation dates
- Null comments
- Various inscription statuses
- Different derogation states

### 2. DocumentGenerator
**Location:** `src/test/java/ma/emsi/inscriptionservice/generators/DocumentGenerator.java`

**Purpose:** Generates random `MultipartFile` documents for testing document validation.

**Features:**
- Generates both valid MIME types (application/pdf, image/jpeg, image/png)
- Generates invalid MIME types for testing rejection
- Produces various file sizes with specific distributions:
  - Very small files (< 1KB) - 10% chance
  - Normal files (1KB - 5MB) - 40% chance
  - Large files (5MB - 9.9MB) - 25% chance
  - Exactly 10MB - 5% chance (boundary case)
  - 10MB + 1 byte - 5% chance (boundary case)
  - Oversized files (> 10MB) - 15% chance
- Generates appropriate file extensions based on MIME type

**Edge Cases:**
- Boundary file sizes (exactly 10MB, 10MB + 1 byte)
- Very small files
- Oversized files
- Mix of valid and invalid MIME types

### 3. InvalidMimeTypeGenerator
**Location:** `src/test/java/ma/emsi/inscriptionservice/generators/InvalidMimeTypeGenerator.java`

**Purpose:** Generates only invalid MIME types for testing validation rejection.

**Features:**
- Office documents (Word, Excel, PowerPoint)
- Text formats (plain text, HTML, JSON, XML, JavaScript)
- Archives (ZIP, RAR, 7z, TAR, GZIP)
- Executables (.exe, .sh, .bat)
- Media files (video, audio)
- Other image formats not allowed (GIF, BMP, SVG, WebP, TIFF)
- Malformed MIME types (empty, incomplete, case variations)

**Edge Cases:**
- Empty strings
- Incomplete MIME types ("application/", "/pdf", "pdf")
- Case variations ("APPLICATION/PDF", "Application/Pdf")
- Trailing/leading spaces
- Common typos ("image/jpg" instead of "image/jpeg")

### 4. DerogationRequestGenerator
**Location:** `src/test/java/ma/emsi/inscriptionservice/generators/DerogationRequestGenerator.java`

**Purpose:** Generates random `DerogationRequest` entities for testing derogation workflows.

**Features:**
- Generates realistic motifs from templates
- Covers all `StatutDerogation` values (EN_ATTENTE, APPROUVE_DIRECTEUR, etc.)
- Generates random dates for request and validation
- Includes optional validator IDs and comments
- Generates optional justification documents (byte arrays)
- Respects length constraints (motif: 2000 chars, comment: 1000 chars)
- Creates minimal associated Inscription entity

**Edge Cases:**
- Null validator IDs (for EN_ATTENTE status)
- Null validation dates
- Null comments
- Null justification documents
- Various approval states

### 5. CampagneGenerator
**Location:** `src/test/java/ma/emsi/inscriptionservice/generators/CampagneGenerator.java`

**Purpose:** Generates random `Campagne` entities for testing campaign management.

**Features:**
- Covers both `TypeCampagne` values (INSCRIPTION, REINSCRIPTION)
- Generates realistic campaign labels with academic years
- Produces various duration scenarios:
  - Normal campaigns (2-3 months) - 40% chance
  - Short campaigns (1 week - 1 month) - 20% chance
  - Long campaigns (6+ months) - 15% chance
  - Past campaigns - 10% chance
  - Future campaigns - 10% chance
  - Same-day campaigns - 3% chance (edge case)
  - Invalid campaigns (dateFin before dateDebut) - 2% chance
- Generates random active flags

**Edge Cases:**
- Same-day campaigns (dateDebut = dateFin)
- Invalid date ranges (dateFin before dateDebut)
- Past campaigns
- Future campaigns
- Very short campaigns
- Very long campaigns

## Test Verification

### GeneratorsTest
**Location:** `src/test/java/ma/emsi/inscriptionservice/generators/GeneratorsTest.java`

A comprehensive test suite that verifies all generators work correctly:

1. **inscriptionGeneratorProducesValidData** - Verifies InscriptionGenerator produces valid inscriptions
2. **documentGeneratorProducesValidData** - Verifies DocumentGenerator produces valid files
3. **invalidMimeTypeGeneratorProducesInvalidTypes** - Verifies InvalidMimeTypeGenerator only produces invalid types
4. **derogationRequestGeneratorProducesValidData** - Verifies DerogationRequestGenerator produces valid requests
5. **campagneGeneratorProducesValidData** - Verifies CampagneGenerator produces valid campaigns
6. **documentGeneratorProducesVariousSizes** - Verifies DocumentGenerator produces files of various sizes
7. **campagneGeneratorProducesVariousDateScenarios** - Verifies CampagneGenerator produces various date scenarios

All tests run with 50-100 trials to ensure comprehensive coverage.

## Documentation

### README.md
**Location:** `src/test/java/ma/emsi/inscriptionservice/generators/README.md`

Comprehensive documentation including:
- Overview of property-based testing
- Detailed description of each generator
- Usage examples with code snippets
- Configuration guidelines
- Edge cases covered
- Best practices for using generators
- Dependencies information

## Usage Example

```java
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class DocumentValidationPropertyTest {

    /**
     * Feature: inscription-service-finalisation, Property 1: MIME type validation
     * Validates: Requirements 1.1
     */
    @Property(trials = 100)
    public void mimeTypeValidation_rejectsInvalidTypes(
        @From(InvalidMimeTypeGenerator.class) String mimeType,
        @From(DocumentGenerator.class) MultipartFile file) {
        
        // Test that invalid MIME types are rejected
        when(file.getContentType()).thenReturn(mimeType);
        
        assertThrows(InvalidDocumentException.class, () -> 
            documentValidationService.validateDocument(file, TypeDocument.CV)
        );
    }
}
```

## Integration with Property-Based Tests

These generators are designed to be used with the 42 correctness properties defined in the design document. Each property test should:

1. Use the `@Property` annotation with `trials = 100` (minimum)
2. Include a comment referencing the property number and requirement
3. Use `@From(GeneratorClass.class)` to specify which generator to use
4. Test the property across all generated inputs

## Files Created

1. `InscriptionGenerator.java` - 120 lines
2. `DocumentGenerator.java` - 140 lines
3. `InvalidMimeTypeGenerator.java` - 100 lines
4. `DerogationRequestGenerator.java` - 130 lines
5. `CampagneGenerator.java` - 160 lines
6. `GeneratorsTest.java` - 140 lines
7. `README.md` - 200 lines

**Total:** 7 files, ~990 lines of code

## Compilation Status

All generators compile successfully with no errors or warnings:
- ✅ InscriptionGenerator.java
- ✅ DocumentGenerator.java
- ✅ InvalidMimeTypeGenerator.java
- ✅ DerogationRequestGenerator.java
- ✅ CampagneGenerator.java
- ✅ GeneratorsTest.java

## Next Steps

These generators are now ready to be used in property-based tests for:
- Document validation (Properties 1-3)
- Attestation generation (Properties 4-10)
- Derogation workflow (Properties 11-17)
- Duration alerts (Properties 18-23)
- Dashboard functionality (Properties 24-29)
- Campaign management (Properties 30-33)
- Kafka event publishing (Properties 34-42)

## Requirements Coverage

This task addresses the requirement: "Ensure generators produce valid and edge-case data" by:
- ✅ Creating generators for all major domain entities
- ✅ Including comprehensive edge cases in each generator
- ✅ Providing various data distributions (normal, boundary, invalid)
- ✅ Respecting entity constraints and relationships
- ✅ Documenting usage and best practices
- ✅ Verifying generators work correctly through tests

## Conclusion

Task 24 has been successfully completed. All custom generators for property-based testing have been implemented, tested, and documented. They are ready to be used in implementing the 42 correctness properties defined in the design document.
