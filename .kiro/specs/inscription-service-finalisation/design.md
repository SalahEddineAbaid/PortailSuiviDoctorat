# Design Document

## Overview

This design document outlines the architecture and implementation approach for finalizing the inscription-service microservice. The service manages doctoral student registrations, re-registrations, and their validation workflow. This finalization adds critical features including automatic document validation, PDF attestation generation, formal derogation workflow, proactive duration alerts, enhanced dashboard capabilities, and improved campaign management.

The service follows a microservices architecture pattern, integrating with user-service via Feign Client and notification-service via Kafka event streaming. It uses MariaDB for persistence and implements a multi-stage validation workflow (Directeur → PED/Admin).

## Architecture

### System Context

The inscription-service operates within a larger microservices ecosystem:

- **User-Service**: Provides user information (students, directors) via REST API
- **Notification-Service**: Consumes events from Kafka to send notifications
- **Gateway-Service**: Routes external requests to inscription-service
- **Eureka-Server**: Service discovery and registration
- **Config-Server**: Centralized configuration management

### Component Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Inscription-Service                       │
├─────────────────────────────────────────────────────────────┤
│  Controllers Layer                                           │
│  ├── InscriptionController                                   │
│  ├── CampagneController                                      │
│  └── DocumentController                                      │
├─────────────────────────────────────────────────────────────┤
│  Service Layer                                               │
│  ├── InscriptionService                                      │
│  ├── DocumentValidationService (NEW)                         │
│  ├── AttestationPdfGenerator (NEW)                           │
│  ├── DerogationService (NEW)                                 │
│  ├── AlerteService (NEW)                                     │
│  ├── DashboardService (NEW)                                  │
│  ├── CampagneService (ENHANCED)                              │
│  └── NotificationService                                     │
├─────────────────────────────────────────────────────────────┤
│  Repository Layer                                            │
│  ├── InscriptionRepository                                   │
│  ├── DerogationRequestRepository (NEW)                       │
│  ├── AlerteDureeRepository (NEW)                             │
│  ├── DocumentGenereRepository (NEW)                          │
│  └── CampagneRepository                                      │
├─────────────────────────────────────────────────────────────┤
│  Integration Layer                                           │
│  ├── UserServiceClient (Feign)                               │
│  └── KafkaProducer                                           │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack

- **Framework**: Spring Boot 3.5.6
- **Database**: MariaDB
- **ORM**: Spring Data JPA / Hibernate
- **Service Discovery**: Netflix Eureka Client
- **HTTP Client**: OpenFeign
- **Messaging**: Spring Kafka
- **PDF Generation**: iText 7.2.5
- **QR Code**: ZXing (to be added)
- **Security**: Spring Security with JWT
- **Build Tool**: Maven


## Components and Interfaces

### 1. DocumentValidationService

**Purpose**: Validates uploaded documents for MIME type, size, and optionally virus scanning.

**Key Methods**:
```java
public class DocumentValidationService {
    void validateDocument(MultipartFile file, TypeDocument type);
    String generateSecureFileName(TypeDocument type, Long userId);
    boolean isMimeTypeAllowed(String mimeType);
    boolean isFileSizeValid(long size);
    void scanForVirus(MultipartFile file); // Optional
}
```

**Configuration Properties**:
- `upload.allowed-types`: Comma-separated list of allowed MIME types
- `upload.max-size`: Maximum file size in bytes
- `upload.virus-scan.enabled`: Enable/disable virus scanning
- `upload.virus-scan.clamav.host`: ClamAV server host
- `upload.virus-scan.clamav.port`: ClamAV server port

**Error Handling**:
- Throws `InvalidDocumentException` with specific error messages
- Error messages are localized and user-friendly

### 2. AttestationPdfGenerator

**Purpose**: Generates official PDF attestation documents for validated inscriptions.

**Key Methods**:
```java
public class AttestationPdfGenerator {
    byte[] generateAttestation(Inscription inscription, InfosDoctorant infos, UserDTO directeur);
    String generateQRCode(String url);
    void addLogo(Document document);
    void addSignature(Document document);
}
```

**PDF Structure**:
- Header: Institution logo and QR code
- Title: "ATTESTATION D'INSCRIPTION"
- Body: Student information, thesis details, director information
- Footer: Validation date, signature, and official stamp

**Dependencies**:
- iText 7 for PDF generation
- ZXing for QR code generation
- Resource files: logo-etablissement.png, signature-chef.png

### 3. DerogationService

**Purpose**: Manages the complete derogation request workflow for students exceeding 3 years.

**Key Methods**:
```java
public class DerogationService {
    DerogationRequest creerDerogation(Long inscriptionId, String motif, byte[] documents);
    DerogationRequest validerParDirecteur(Long derogationId, boolean approuve, String commentaire);
    DerogationRequest validerParPED(Long derogationId, boolean approuve, String commentaire);
    DerogationRequest getDerogation(Long inscriptionId);
    boolean isDerogationRequise(Inscription inscription);
}
```

**Workflow States**:
1. EN_ATTENTE → Student submits request
2. APPROUVE_DIRECTEUR → Director approves
3. EN_ATTENTE_PED → Awaiting PED decision
4. APPROUVE_PED → Final approval (allows re-registration)
5. REJETE → Rejected (blocks re-registration)

### 4. AlerteService

**Purpose**: Monitors student duration and generates proactive alerts.

**Key Methods**:
```java
public class AlerteService {
    void verifierEtGenererAlertes(Inscription inscription);
    List<AlerteDuree> getAlertesActives(Long doctorantId);
    boolean alerteExiste(Inscription inscription, TypeAlerte type);
    void creerAlerte(Inscription inscription, TypeAlerte type);
}
```

**Alert Thresholds**:
- 2.5 years: APPROCHE_3_ANS
- 5.5 years: APPROCHE_6_ANS
- 6.0 years: DEPASSE_6_ANS (blocks re-registration)

**Configuration Properties**:
- `alertes.duree.seuil-3-ans`: Threshold for 3-year warning (default: 2.5)
- `alertes.duree.seuil-6-ans`: Threshold for 6-year warning (default: 5.5)

### 5. DashboardService

**Purpose**: Provides consolidated dashboard data for doctoral students.

**Key Methods**:
```java
public class DashboardService {
    DashboardResponse getDashboardDoctorant(Long doctorantId);
    List<DocumentManquant> getDocumentsManquants(Long inscriptionId);
    StatistiquesDossier calculerStatistiques(Long inscriptionId);
    Milestone getProchaineMilestone(Long doctorantId);
}
```

**Dashboard Response Structure**:
```json
{
  "doctorant": { "id", "nom", "prenom", "email" },
  "inscriptionCourante": { "id", "annee", "type", "statut", "dureeDoctorat", "derogationActive" },
  "historiqueInscriptions": [...],
  "alertes": [...],
  "documentsManquants": [...],
  "prochaineMilestone": { "type", "dateEcheance", "statut" },
  "statistiques": { "tauxCompletionDossier", "documentsValides", "documentsTotal" }
}
```

### 6. CampagneService (Enhanced)

**Purpose**: Manages registration campaigns with automated notifications and statistics.

**New Methods**:
```java
public class CampagneService {
    @Scheduled(cron = "0 0 8 * * ?")
    void verifierCampagnes();
    
    StatistiquesCampagne getStatistiques(Long campagneId);
    Campagne clonerCampagne(Long campagneId, LocalDate dateDebut, LocalDate dateFin);
}
```

**Statistics Calculation**:
- Total inscriptions
- Breakdown by status (BROUILLON, SOUMIS, VALIDE, REJETE)
- Validation rate percentage
- Average validation time in days


## Data Models

### New Entities

#### DerogationRequest
```java
@Entity
@Table(name = "derogation_requests")
public class DerogationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;
    
    @Column(nullable = false, length = 2000)
    private String motif;
    
    @Column(nullable = false)
    private LocalDateTime dateDemande;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDerogation statut;
    
    private Long validateurId;
    
    @Column(length = 1000)
    private String commentaireValidation;
    
    private LocalDateTime dateValidation;
    
    @Lob
    private byte[] documentsJustificatifs;
}

enum StatutDerogation {
    EN_ATTENTE,
    APPROUVE_DIRECTEUR,
    EN_ATTENTE_PED,
    APPROUVE_PED,
    REJETE
}
```

#### AlerteDuree
```java
@Entity
@Table(name = "alertes_duree")
public class AlerteDuree {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAlerte type;
    
    @Column(nullable = false)
    private LocalDateTime dateAlerte;
    
    @Column(nullable = false)
    private boolean traite;
    
    @Column(length = 500)
    private String action;
}

enum TypeAlerte {
    APPROCHE_3_ANS,
    APPROCHE_6_ANS,
    DEPASSE_6_ANS
}
```

#### DocumentGenere
```java
@Entity
@Table(name = "documents_generes")
public class DocumentGenere {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeDocumentGenere type;
    
    @Column(nullable = false, length = 500)
    private String cheminFichier;
    
    @Column(nullable = false)
    private LocalDateTime dateGeneration;
    
    @Column(nullable = false)
    private Long tailleFichier;
}

enum TypeDocumentGenere {
    ATTESTATION_INSCRIPTION,
    PROCES_VERBAL,
    AUTORISATION_SOUTENANCE
}
```

### Enhanced Entities

#### Inscription (additions)
```java
@Entity
public class Inscription {
    // Existing fields...
    
    @Column(nullable = false)
    private boolean bloqueReInscription = false;
    
    @OneToMany(mappedBy = "inscription", cascade = CascadeType.ALL)
    private List<DerogationRequest> derogations;
    
    @OneToMany(mappedBy = "inscription", cascade = CascadeType.ALL)
    private List<AlerteDuree> alertes;
    
    @OneToMany(mappedBy = "inscription", cascade = CascadeType.ALL)
    private List<DocumentGenere> documentsGeneres;
}
```

### DTOs

#### DashboardResponse
```java
public class DashboardResponse {
    private DoctorantInfo doctorant;
    private InscriptionCourante inscriptionCourante;
    private List<InscriptionHistorique> historiqueInscriptions;
    private List<AlerteInfo> alertes;
    private List<DocumentManquant> documentsManquants;
    private Milestone prochaineMilestone;
    private StatistiquesDossier statistiques;
}
```

#### DerogationRequest DTO
```java
public class DerogationRequestDTO {
    private String motif;
    private MultipartFile documentsJustificatifs;
}

public class DerogationValidationDTO {
    private boolean approuve;
    private String commentaire;
}
```

#### StatistiquesCampagne
```java
public class StatistiquesCampagne {
    private Long campagneId;
    private String libelle;
    private TypeInscription type;
    private int nombreInscriptions;
    private Map<StatutInscription, Integer> parStatut;
    private double tauxValidation;
    private double tempsMoyenValidation; // in days
}
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Document Validation Properties

**Property 1: MIME type validation**
*For any* uploaded document, if the MIME type is not in the allowed types list (application/pdf, image/jpeg, image/png), then the validation should reject the document
**Validates: Requirements 1.1**

**Property 2: File size validation**
*For any* uploaded document, if the file size exceeds 10 MB, then the validation should reject the document
**Validates: Requirements 1.2**

**Property 3: File naming consistency**
*For any* valid uploaded document, the renamed file should match the pattern {type}_{timestamp}_{userId}_{random}.{extension}
**Validates: Requirements 1.5**

### Attestation Generation Properties

**Property 4: Attestation generation trigger**
*For any* inscription that transitions to VALIDE status, an attestation PDF should be generated
**Validates: Requirements 2.1**

**Property 5: Attestation completeness**
*For any* generated attestation, the PDF content should include student name, CIN, CNE, year of study, academic year, thesis subject, and director information
**Validates: Requirements 2.2**

**Property 6: QR code embedding**
*For any* generated attestation, the PDF should contain a QR code with the verification URL pattern https://portail.emsi.ma/verify/attestation/{id}
**Validates: Requirements 2.3**

**Property 7: Attestation storage**
*For any* generated attestation, the file should exist at path ./uploads/attestations/{inscriptionId}_{timestamp}.pdf
**Validates: Requirements 2.4**

**Property 8: Attestation database record**
*For any* generated attestation, a corresponding record should exist in the documents_generes table with the correct file path
**Validates: Requirements 2.5**

**Property 9: Attestation download authorization**
*For any* attestation download request, access should be granted only if the requester is the student, their director, or an administrator
**Validates: Requirements 2.6**

**Property 10: Attestation content type**
*For any* authorized attestation download, the response Content-Type should be application/pdf
**Validates: Requirements 2.7**

### Derogation Workflow Properties

**Property 11: Derogation creation**
*For any* submitted derogation request, a DerogationRequest entity should be created with status EN_ATTENTE
**Validates: Requirements 3.1**

**Property 12: Derogation notification to director**
*For any* created derogation request, a Kafka notification event should be published to the thesis director
**Validates: Requirements 3.2**

**Property 13: Director approval workflow**
*For any* derogation request approved by a director, the status should update to APPROUVE_DIRECTEUR and a notification should be sent to PED administrators
**Validates: Requirements 3.3**

**Property 14: Director rejection workflow**
*For any* derogation request rejected by a director, the status should update to REJETE and a notification should be sent to the student
**Validates: Requirements 3.4**

**Property 15: PED approval workflow**
*For any* derogation request approved by PED, the status should update to APPROUVE_PED and the inscription derogation flag should be set to true
**Validates: Requirements 3.5**

**Property 16: PED rejection workflow**
*For any* derogation request rejected by PED, the status should update to REJETE and the inscription bloqueReInscription flag should be set to true
**Validates: Requirements 3.6**

**Property 17: Re-registration derogation requirement**
*For any* re-registration attempt where duration exceeds 3 years and no approved derogation exists, the registration should be rejected
**Validates: Requirements 3.7**

### Duration Alert Properties

**Property 18: 3-year approach alert**
*For any* inscription where calculated duration reaches 2.5 years, an alert of type APPROCHE_3_ANS should be created
**Validates: Requirements 4.1**

**Property 19: 6-year approach alert**
*For any* inscription where calculated duration reaches 5.5 years, an alert of type APPROCHE_6_ANS should be created
**Validates: Requirements 4.2**

**Property 20: 6-year exceeded alert and blocking**
*For any* inscription where calculated duration reaches 6 years, an alert of type DEPASSE_6_ANS should be created and bloqueReInscription should be set to true
**Validates: Requirements 4.3**

**Property 21: Alert notification publishing**
*For any* created alert, a corresponding Kafka notification event should be published with alert details
**Validates: Requirements 4.4**

**Property 22: Alert idempotency**
*For any* inscription, calling verifierEtGenererAlertes multiple times should not create duplicate alerts of the same type
**Validates: Requirements 4.5**

**Property 23: Re-registration alert verification**
*For any* submitted re-registration, the alert verification process should be triggered
**Validates: Requirements 4.6**

### Dashboard Properties

**Property 24: Dashboard current inscription**
*For any* dashboard request, the response should include current inscription with year, type, status, duration, and derogation status
**Validates: Requirements 5.1**

**Property 25: Dashboard inscription history**
*For any* dashboard request, the response should include all inscriptions for the student ordered by year ascending
**Validates: Requirements 5.2**

**Property 26: Dashboard active alerts**
*For any* dashboard request, the response should include all active alerts with type, date, and message
**Validates: Requirements 5.3**

**Property 27: Dashboard missing documents**
*For any* dashboard request, the response should identify all required documents that are not yet uploaded
**Validates: Requirements 5.4**

**Property 28: Dashboard statistics calculation**
*For any* dashboard request, the statistics should correctly calculate completion rate as (validated documents / total required documents) * 100
**Validates: Requirements 5.5**

**Property 29: Dashboard milestone calculation**
*For any* dashboard request, the next milestone should be determined based on current inscription status and academic calendar
**Validates: Requirements 5.6**

### Campaign Management Properties

**Property 30: Campaign opening notification**
*For any* campaign where the start date matches the current date, opening notifications should be sent to all eligible students
**Validates: Requirements 6.1**

**Property 31: Campaign closing automation**
*For any* campaign where the end date matches the current date, closing notifications should be sent and the active flag should be set to false
**Validates: Requirements 6.2**

**Property 32: Campaign statistics accuracy**
*For any* campaign, the statistics should correctly count total inscriptions, breakdown by status, and calculate validation rate as (VALIDE count / total count) * 100
**Validates: Requirements 6.3**

**Property 33: Campaign cloning**
*For any* cloned campaign, the new campaign should have the same type and libelle with year incremented by 1
**Validates: Requirements 6.4**

### Kafka Event Publishing Properties

**Property 34: Inscription submission event**
*For any* submitted inscription, an INSCRIPTION_SOUMISE event should be published to Kafka with inscriptionId, doctorantId, and directeurId
**Validates: Requirements 7.1**

**Property 35: Director validation event**
*For any* inscription validated by a director, an INSCRIPTION_VALIDEE_DIRECTEUR event should be published to Kafka
**Validates: Requirements 7.2**

**Property 36: Director rejection event**
*For any* inscription rejected by a director, an INSCRIPTION_REJETEE_DIRECTEUR event should be published to Kafka with rejection reason
**Validates: Requirements 7.3**

**Property 37: Admin validation event**
*For any* inscription validated by an administrator, an INSCRIPTION_VALIDEE_ADMIN event should be published to Kafka
**Validates: Requirements 7.4**

**Property 38: Admin rejection event**
*For any* inscription rejected by an administrator, an INSCRIPTION_REJETEE_ADMIN event should be published to Kafka with rejection reason
**Validates: Requirements 7.5**

**Property 39: Campaign opened event**
*For any* campaign that opens, a CAMPAGNE_OUVERTE event should be published to Kafka
**Validates: Requirements 7.6**

**Property 40: Campaign closed event**
*For any* campaign that closes, a CAMPAGNE_FERMEE event should be published to Kafka
**Validates: Requirements 7.7**

**Property 41: Derogation requested event**
*For any* submitted derogation request, a DEROGATION_DEMANDEE event should be published to Kafka
**Validates: Requirements 7.8**

**Property 42: Alert duration event**
*For any* created duration alert, an ALERTE_DUREE event should be published to Kafka
**Validates: Requirements 7.9**


## Error Handling

### Exception Hierarchy

```java
// Base exception
public class InscriptionServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
}

// Specific exceptions
public class InvalidDocumentException extends InscriptionServiceException {
    // For document validation failures
}

public class DerogationRequiredException extends InscriptionServiceException {
    // When re-registration requires derogation
}

public class DurationLimitExceededException extends InscriptionServiceException {
    // When 6-year limit is exceeded
}

public class UnauthorizedAccessException extends InscriptionServiceException {
    // For authorization failures
}

public class CampagneClosedException extends InscriptionServiceException {
    // When attempting to register in closed campaign
}
```

### Error Response Format

```json
{
  "timestamp": "2024-12-30T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "INVALID_DOCUMENT_TYPE",
  "message": "Le fichier doit être au format PDF ou image (JPEG/PNG)",
  "path": "/api/inscriptions/documents/upload"
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocument(InvalidDocumentException ex);
    
    @ExceptionHandler(DerogationRequiredException.class)
    public ResponseEntity<ErrorResponse> handleDerogationRequired(DerogationRequiredException ex);
    
    @ExceptionHandler(DurationLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDurationLimit(DurationLimitExceededException ex);
    
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedAccessException ex);
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex);
}
```

### Validation Error Messages

All error messages should be:
- Clear and actionable
- Localized (French for this system)
- Specific to the validation failure
- Include guidance on how to fix the issue

Examples:
- "Le fichier doit être au format PDF ou image (JPEG/PNG)"
- "La taille du fichier ne doit pas dépasser 10 MB"
- "Une dérogation est requise pour une réinscription après 3 ans"
- "La durée maximale de 6 ans est dépassée, réinscription impossible"

### Retry and Circuit Breaker

For external service calls (Feign Client, Kafka):

```java
@CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
@Retry(name = "userService", fallbackMethod = "getUserFallback")
public UserDTO getUser(Long userId) {
    return userServiceClient.getUser(userId);
}

private UserDTO getUserFallback(Long userId, Exception ex) {
    log.error("Failed to fetch user {}: {}", userId, ex.getMessage());
    // Return cached data or throw appropriate exception
}
```

Configuration:
```properties
resilience4j.circuitbreaker.instances.userService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.userService.wait-duration-in-open-state=30s
resilience4j.retry.instances.userService.max-attempts=3
resilience4j.retry.instances.userService.wait-duration=1s
```


## Testing Strategy

### Unit Testing

Unit tests will verify specific functionality of individual components:

**DocumentValidationService Tests**:
- Test MIME type validation with valid and invalid types
- Test file size validation at boundary conditions (exactly 10MB, 10MB + 1 byte)
- Test file naming pattern generation
- Test virus scanning integration (when enabled)

**AttestationPdfGenerator Tests**:
- Test PDF generation with complete inscription data
- Test QR code generation and embedding
- Test logo and signature placement
- Test handling of missing optional fields

**DerogationService Tests**:
- Test derogation creation workflow
- Test state transitions for each validation step
- Test notification triggering at each stage
- Test rejection handling

**AlerteService Tests**:
- Test alert creation at specific duration thresholds
- Test duplicate alert prevention
- Test alert notification publishing

**DashboardService Tests**:
- Test dashboard data aggregation
- Test statistics calculation with various document states
- Test milestone determination logic

**CampagneService Tests**:
- Test campaign statistics calculation
- Test campaign cloning logic
- Test scheduled task execution

### Property-Based Testing

Property-based tests will verify universal properties across all inputs using **JUnit QuickCheck** library.

**Configuration**: Each property test should run a minimum of 100 iterations to ensure comprehensive coverage.

**Test Tagging**: Each property-based test must include a comment explicitly referencing the correctness property using this format:
```java
/**
 * Feature: inscription-service-finalisation, Property 1: MIME type validation
 * Validates: Requirements 1.1
 */
```

**Property Test Examples**:

```java
@Property(trials = 100)
public void mimeTypeValidation_rejectsInvalidTypes(
    @ForAll @From(DocumentGenerator.class) MultipartFile file,
    @ForAll @From(InvalidMimeTypeGenerator.class) String mimeType) {
    
    // Property 1: For any document with invalid MIME type, validation should reject
    when(file.getContentType()).thenReturn(mimeType);
    
    assertThrows(InvalidDocumentException.class, () -> 
        documentValidationService.validateDocument(file, TypeDocument.CV)
    );
}

@Property(trials = 100)
public void attestationGeneration_triggeredOnValidation(
    @ForAll @From(InscriptionGenerator.class) Inscription inscription) {
    
    // Property 4: For any inscription transitioning to VALIDE, attestation should be generated
    inscription.setStatut(StatutInscription.EN_ATTENTE_ADMIN);
    inscriptionService.validerParAdmin(inscription.getId(), validationRequest);
    
    verify(attestationPdfGenerator, times(1)).generateAttestation(any(), any(), any());
}

@Property(trials = 100)
public void alertIdempotency_noDuplicates(
    @ForAll @From(InscriptionGenerator.class) Inscription inscription) {
    
    // Property 22: Calling alert verification multiple times should not create duplicates
    alerteService.verifierEtGenererAlertes(inscription);
    alerteService.verifierEtGenererAlertes(inscription);
    alerteService.verifierEtGenererAlertes(inscription);
    
    List<AlerteDuree> alertes = alerteDureeRepository.findByInscriptionId(inscription.getId());
    Set<TypeAlerte> uniqueTypes = alertes.stream()
        .map(AlerteDuree::getType)
        .collect(Collectors.toSet());
    
    assertEquals(uniqueTypes.size(), alertes.size());
}
```

**Custom Generators**:

```java
public class InscriptionGenerator implements ArbitraryGenerator<Inscription> {
    @Override
    public Inscription generate(SourceOfRandomness random, GenerationStatus status) {
        return Inscription.builder()
            .doctorantId(random.nextLong(1, 10000))
            .directeurTheseId(random.nextLong(1, 1000))
            .type(random.choose(TypeInscription.values()))
            .anneeInscription(random.nextInt(2020, 2025))
            .statut(random.choose(StatutInscription.values()))
            .datePremiereInscription(generateRandomDate(random))
            .build();
    }
}

public class DocumentGenerator implements ArbitraryGenerator<MultipartFile> {
    @Override
    public MultipartFile generate(SourceOfRandomness random, GenerationStatus status) {
        byte[] content = new byte[random.nextInt(1, 10 * 1024 * 1024)];
        random.nextBytes(content);
        
        String mimeType = random.choose(
            "application/pdf", "image/jpeg", "image/png",
            "application/msword", "text/plain" // Include invalid types
        );
        
        return new MockMultipartFile("file", "test.pdf", mimeType, content);
    }
}
```

### Integration Testing

Integration tests will verify end-to-end workflows:

**Inscription Workflow Test**:
1. Create inscription
2. Upload documents
3. Submit for validation
4. Director validates
5. Admin validates
6. Verify attestation generated
7. Verify Kafka events published

**Derogation Workflow Test**:
1. Create inscription with > 3 years duration
2. Submit derogation request
3. Director approves
4. PED approves
5. Verify re-registration allowed

**Campaign Lifecycle Test**:
1. Create campaign
2. Simulate date reaching start date
3. Verify notifications sent
4. Create inscriptions
5. Simulate date reaching end date
6. Verify campaign closed

### Test Database

Use H2 in-memory database for tests:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
```

### Mocking Strategy

- Mock external services (UserServiceClient, Kafka) in unit tests
- Use TestContainers for integration tests requiring real database
- Use embedded Kafka for integration tests requiring message publishing

### Test Coverage Goals

- Unit test coverage: > 80%
- Property test coverage: All 42 correctness properties
- Integration test coverage: All major workflows
- Critical paths: 100% coverage (validation, derogation, attestation generation)

