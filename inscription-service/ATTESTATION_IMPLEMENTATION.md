# Attestation PDF Generation Implementation

## Overview
This document describes the implementation of the attestation PDF generation service for the inscription-service.

## Implementation Summary

### 1. AttestationPdfGenerator Service
**Location**: `src/main/java/ma/emsi/inscriptionservice/services/AttestationPdfGenerator.java`

**Key Features**:
- Generates official PDF attestation documents for validated inscriptions
- Uses iText 7 for PDF generation
- Uses ZXing for QR code generation
- Includes institution logo and director signature
- Stores generated PDFs in configurable directory
- Creates database records for generated documents

**Main Method**:
```java
public String generateAttestation(Inscription inscription, InfosDoctorant infosDoctorant, UserDTO directeur)
```

**PDF Structure**:
1. **Header**: Institution logo (left) and QR code (right)
2. **Title**: "ATTESTATION D'INSCRIPTION" with academic year
3. **Student Information**: Name, CIN, CNE, year of study, inscription type
4. **Thesis Details**: Subject, discipline, laboratory, establishment
5. **Director Information**: Director's full name
6. **Signature Section**: Stamp placeholder and director's signature with validation date
7. **Footer**: Validity note

**QR Code**:
- Contains verification URL: `https://portail.emsi.ma/verify/attestation/{inscriptionId}`
- Size: 200x200 pixels
- Format: PNG

**File Storage**:
- Directory: `./uploads/attestations/`
- Filename pattern: `attestation_{inscriptionId}_{timestamp}.pdf`
- Timestamp format: `yyyyMMdd_HHmmss`

**Database Record**:
- Creates `DocumentGenere` entity with:
  - Type: `ATTESTATION_INSCRIPTION`
  - File path
  - File size
  - Generation timestamp

### 2. NotificationService Integration
**Location**: `src/main/java/ma/emsi/inscriptionservice/services/NotificationService.java`

**Updated Method**:
```java
public void genererAttestationInscription(Long inscriptionId)
```

**Integration Flow**:
1. Retrieves inscription with all related information
2. Fetches director information from UserServiceClient
3. Calls AttestationPdfGenerator to create PDF
4. Logs success or handles errors gracefully
5. Does not block validation process if generation fails

### 3. Configuration Properties
**Location**: `src/main/resources/application.properties`

**New Properties**:
```properties
# PDF Generation Configuration
pdf.logo.path=classpath:static/images/logo-etablissement.png
pdf.signature.path=classpath:static/images/signature-chef.png
pdf.qrcode.base-url=https://portail.emsi.ma/verify/attestation/
pdf.attestation.output-dir=./uploads/attestations
```

### 4. Resource Files
**Location**: `src/main/resources/static/images/`

**Required Images**:
1. `logo-etablissement.png` (100x100 pixels, PNG with transparency)
2. `signature-chef.png` (150x75 pixels, PNG with transparency)

**Note**: If images are not present, the service uses text placeholders and logs warnings.

## Dependencies
All required dependencies were already configured in `pom.xml`:
- iText 7.2.5 (PDF generation)
- ZXing 3.5.3 (QR code generation)
- Spring Boot (framework)
- Lombok (boilerplate reduction)

## Error Handling
- Graceful degradation if images cannot be loaded (uses text placeholders)
- Comprehensive logging at all stages
- Does not block validation workflow if attestation generation fails
- Attestation can be regenerated later if initial generation fails

## Integration Points

### Automatic Generation
Attestation is automatically generated when:
- Admin validates an inscription (`InscriptionService.validerParAdmin`)
- Inscription status changes to `VALIDE`
- Called via `NotificationService.genererAttestationInscription()`

### Manual Generation
Can be triggered manually by:
- Calling `AttestationPdfGenerator.generateAttestation()` directly
- Useful for regenerating lost or corrupted attestations

## Testing Considerations
- Unit tests should mock UserServiceClient and ResourceLoader
- Integration tests should use test resources for logo/signature
- Property-based tests should verify PDF structure and content
- Test with missing images to verify graceful degradation

## Future Enhancements
1. Add attestation download endpoint (Task 5)
2. Add authorization checks for downloads
3. Implement attestation verification via QR code
4. Add multilingual support
5. Add digital signature for authenticity
6. Implement attestation templates for different types

## Compliance
This implementation satisfies the following requirements:
- **Requirement 2.1**: Attestation generated when status changes to VALIDE
- **Requirement 2.2**: Includes all required student and thesis information
- **Requirement 2.3**: Embeds QR code with verification URL
- **Requirement 2.4**: Stores PDF at specified path
- **Requirement 2.5**: Creates DocumentGenere database record

## Status
✅ **COMPLETE** - All core functionality implemented and integrated
