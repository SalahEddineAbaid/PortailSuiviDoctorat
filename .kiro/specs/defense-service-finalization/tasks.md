# Implementation Plan: Defense Service Finalization

## Overview

This implementation plan breaks down the defense-service finalization into discrete coding tasks. The tasks build incrementally, starting with data models, then services, controllers, and finally integration components. Each task includes specific requirements references and testing sub-tasks.

## Tasks

- [x] 1. Create Publication Management Data Models

  - Create `Publication` entity with all fields (id, prerequisites, titre, journal, anneePublication, type, quartile, doi, url, valide, validateurId, commentaireValidation, dateValidation)
  - Create `TypePublication` enum (JOURNAL_ARTICLE, CONFERENCE, CHAPITRE_LIVRE, BREVET)
  - Create `QuartileJournal` enum (Q1, Q2, Q3, Q4, NON_CLASSE)
  - Create `PublicationRepository` interface extending JpaRepository with custom query methods
  - Add `@OneToMany` relationship to `Prerequisites` entity for publications
  - Create Flyway migration script for publications table
  - _Requirements: 1.1, 1.2, 1.7_

- [ ]\* 1.1 Write property test for publication persistence

  - **Property 1: Publication Persistence Completeness**
  - **Validates: Requirements 1.1**

- [ ]\* 1.2 Write property test for default validation state

  - **Property 2: Publication Default Validation State**
  - **Validates: Requirements 1.2**

- [x] 2. Implement Publication Service Layer

  - [x] 2.1 Create PublicationService interface and implementation

    - Implement `createPublication(PublicationCreateDTO)` method
    - Implement `getPublicationsByPrerequisites(Long)` method
    - Implement `validatePublication(Long, ValidationDTO)` method
    - Implement `getPendingValidations()` method
    - Implement `hasValidQ1Q2Publications(Long, int)` method
    - Implement `countValidQ1Q2Publications(Long)` method
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.8_

  - [ ]\* 2.2 Write property test for validation data completeness

    - **Property 3: Publication Validation Data Completeness**
    - **Validates: Requirements 1.3, 1.8**

  - [ ]\* 2.3 Write property test for Q1/Q2 counting accuracy

    - **Property 4: Q1/Q2 Publication Counting Accuracy**
    - **Validates: Requirements 1.4**

  - [ ]\* 2.4 Write property test for pending validation query

    - **Property 5: Pending Validation Query Correctness**
    - **Validates: Requirements 1.5**

  - [ ]\* 2.5 Write property test for prerequisites Q1/Q2 validation

    - **Property 6: Prerequisites Q1/Q2 Validation Rule**
    - **Validates: Requirements 1.6**

  - [ ]\* 2.6 Write property test for journal article quartile requirement
    - **Property 7: Journal Article Quartile Requirement**
    - **Validates: Requirements 1.7**

- [x] 3. Create Publication DTOs and Mappers

  - Create `PublicationCreateDTO` request DTO
  - Create `PublicationResponseDTO` response DTO
  - Create `ValidationDTO` request DTO for validation
  - Create `PublicationMapper` for entity-DTO conversion
  - _Requirements: 1.1, 1.3_

- [x] 4. Implement Publication REST Controller

  - Create `PublicationController` with endpoints:
    - `POST /api/defense-service/publications`
    - `GET /api/defense-service/publications/prerequisites/{id}`
    - `PUT /api/defense-service/publications/{id}/valider`
    - `GET /api/defense-service/publications/en-attente-validation`
  - Add request validation annotations
  - Add error handling with appropriate HTTP status codes
  - _Requirements: 1.1, 1.3, 1.5_

- [ ]\* 4.1 Write unit tests for publication controller

  - Test all endpoints with valid and invalid inputs
  - Test error responses
  - _Requirements: 1.1, 1.3, 1.5_

- [x] 5. Create Authorization Workflow Data Models

  - Create `AutorisationSoutenance` entity with all fields
  - Create `StatutAutorisation` enum (EN_ATTENTE, AUTORISE, REFUSE)
  - Create `AutorisationSoutenanceRepository` interface
  - Create Flyway migration script for autorisations_soutenance table
  - _Requirements: 2.4, 2.8, 2.12_

- [-] 6. Implement Authorization Service Layer

  - [x] 6.1 Create AutorisationService interface and implementation

    - Implement `verifierPrerequisAutorisation(Long)` method with all four checks
    - Implement `autoriser(Long, AutorisationDTO)` method
    - Implement `refuser(Long, RefusDTO)` method
    - Implement `getAutorisation(Long)` method
    - Implement helper methods for each verification check
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.8, 2.9, 2.11, 2.12_

  - [ ]\* 6.2 Write property test for authorization verification completeness

    - **Property 8: Authorization Verification Completeness**
    - **Validates: Requirements 2.1, 2.11**

  - [ ]\* 6.3 Write property test for jury completeness validation

    - **Property 9: Jury Completeness Validation**
    - **Validates: Requirements 2.2**

  - [ ]\* 6.4 Write property test for rapporteur reports validation

    - **Property 10: Rapporteur Reports Validation**
    - **Validates: Requirements 2.3**

  - [ ]\* 6.5 Write property test for authorization state transitions

    - **Property 11: Authorization State Transitions**
    - **Validates: Requirements 2.4, 2.5, 2.6**

  - [ ]\* 6.6 Write property test for refusal state transitions

    - **Property 12: Refusal State Transitions**
    - **Validates: Requirements 2.8, 2.9**

  - [ ]\* 6.7 Write property test for authorization audit trail
    - **Property 13: Authorization Audit Trail**
    - **Validates: Requirements 2.12**

- [x] 7. Create Authorization DTOs

  - Create `VerificationResultDTO` with detailed check results
  - Create `AutorisationDTO` request DTO for authorization
  - Create `RefusDTO` request DTO for refusal
  - Create `AutorisationSoutenanceDTO` response DTO
  - Create `AutorisationMapper` for entity-DTO conversion
  - _Requirements: 2.1, 2.4, 2.8, 2.11_

- [x] 8. Implement Authorization REST Controller

  - Create `AutorisationController` with endpoints:
    - `POST /api/defense-service/autorisation/{defenseRequestId}/verifier`
    - `POST /api/defense-service/autorisation/{defenseRequestId}/autoriser`
    - `POST /api/defense-service/autorisation/{defenseRequestId}/refuser`
    - `GET /api/defense-service/autorisation/{defenseRequestId}`
  - Add request validation and error handling
  - _Requirements: 2.1, 2.4, 2.8_

- [ ]\* 8.1 Write unit tests for authorization controller

  - Test verification endpoint with various defense request states
  - Test authorization and refusal endpoints
  - Test error cases
  - _Requirements: 2.1, 2.4, 2.8_

- [x] 9. Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Implement Procès-Verbal PDF Generation

  - [x] 10.1 Add iText 7 dependency to pom.xml

    - Add iText core and layout dependencies
    - _Requirements: 3.2_

  - [x] 10.2 Create ProcesVerbalPdfGenerator service

    - Implement `generateProcesVerbal(Defense)` method
    - Create PDF with institution header and logo
    - Add doctorant information section
    - Add thesis title section
    - Add defense date/location section
    - Add jury composition table with roles
    - Add defense outcome and mention section
    - Add jury recommendations section
    - Add signature spaces for jury members
    - _Requirements: 3.2, 3.3_

  - [ ]\* 10.3 Write property test for PDF generation
    - **Property 15: Procès-Verbal Generation**
    - **Validates: Requirements 3.2, 3.3**

- [x] 11. Implement Defense Finalization

  - [x] 11.1 Update DefenseService with finalization methods

    - Implement `finalizeDefense(Long, FinalizationDTO)` method
    - Update Defense entity with finalization data
    - Change status to COMPLETED
    - Generate procès-verbal PDF
    - Store PDF URL in Defense entity
    - Implement `getProcesVerbal(Long, Long)` method with access control
    - _Requirements: 3.1, 3.2, 3.5, 3.7, 3.8_

  - [ ]\* 11.2 Write property test for defense finalization data persistence

    - **Property 14: Defense Finalization Data Persistence**
    - **Validates: Requirements 3.1, 3.5**

  - [ ]\* 11.3 Write property test for PV access control

    - **Property 16: Procès-Verbal Access Control**
    - **Validates: Requirements 3.7**

  - [ ]\* 11.4 Write property test for PV availability constraint
    - **Property 17: Procès-Verbal Availability Constraint**
    - **Validates: Requirements 3.8**

- [x] 12. Create Defense Finalization DTOs and Controller Endpoints

  - Create `FinalizationDTO` request DTO
  - Update `DefenseController` with endpoints:
    - `POST /api/defense-service/defenses/{id}/finaliser`
    - `GET /api/defense-service/defenses/{id}/proces-verbal`
  - Add access control checks for PV download
  - _Requirements: 3.1, 3.2, 3.7, 3.8_

- [ ]\* 12.1 Write unit tests for defense finalization endpoints

  - Test finalization with valid data
  - Test PV download with authorized and unauthorized users
  - Test error cases
  - _Requirements: 3.1, 3.7, 3.8_

- [x] 13. Implement Director Dashboard

  - [x] 13.1 Create DirecteurDashboardService

    - Implement `getDashboard(Long)` method
    - Calculate statistics (active doctorants, defenses to plan, pending reports, juries to propose)
    - Retrieve defense requests for directeur's doctorants
    - Retrieve scheduled defenses
    - Generate alerts for overdue reports
    - Enrich data with user information from User_Service
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ]\* 13.2 Write property test for dashboard statistics accuracy

    - **Property 18: Director Dashboard Statistics Accuracy**
    - **Validates: Requirements 4.1**

  - [ ]\* 13.3 Write property test for dashboard data filtering

    - **Property 19: Director Dashboard Data Filtering**
    - **Validates: Requirements 4.2, 4.3**

  - [ ]\* 13.4 Write property test for alert generation

    - **Property 20: Director Dashboard Alert Generation**
    - **Validates: Requirements 4.4, 4.6**

  - [ ]\* 13.5 Write property test for defense request DTO completeness
    - **Property 21: Defense Request DTO Completeness**
    - **Validates: Requirements 4.5**

- [x] 14. Create Director Dashboard DTOs and Controller

  - Create `DirecteurDashboardDTO` response DTO
  - Create `StatistiquesDirecteurDTO` DTO
  - Create `DefenseRequestSummaryDTO` DTO
  - Create `DefenseScheduledDTO` DTO
  - Create `AlerteDTO` DTO
  - Create `DirecteurDashboardController` with endpoint:
    - `GET /api/defense-service/directeur/{directeurId}/dashboard`
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]\* 14.1 Write unit tests for director dashboard controller

  - Test dashboard retrieval with various data scenarios
  - Test filtering and statistics calculations
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 15. Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [-] 16. Implement Statistics and Reporting

  - [x] 16.1 Create StatistiquesService

    - Implement `getStatistiques()` method
    - Calculate defense requests by status
    - Calculate defenses by month
    - Calculate mention distribution
    - Calculate success rate
    - Calculate average duration from start to completion
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]\* 16.2 Write property test for statistics aggregation accuracy

    - **Property 22: Statistics Aggregation Accuracy**
    - **Validates: Requirements 5.1, 5.2, 5.3**

  - [ ]\* 16.3 Write property test for success rate calculation

    - **Property 23: Success Rate Calculation**
    - **Validates: Requirements 5.4**

  - [ ]\* 16.4 Write property test for average duration calculation
    - **Property 24: Average Duration Calculation**
    - **Validates: Requirements 5.5**

- [x] 17. Implement PDF Report Generation

  - [x] 17.1 Create ReportPdfGenerator service

    - Implement `generateMonthlyReport(YearMonth)` method
    - Implement `generateAnnualReport(int)` method
    - Create PDF templates with charts and tables
    - Include defense lists, statistics, and visualizations
    - _Requirements: 5.6, 5.7_

  - [ ]\* 17.2 Write unit tests for report generation
    - Test monthly report generation
    - Test annual report generation
    - _Requirements: 5.6, 5.7_

- [x] 18. Create Statistics DTOs and Controller

  - Create `StatistiquesDTO` response DTO
  - Create `DefenseCountByMonthDTO` DTO
  - Create `StatistiquesController` with endpoints:
    - `GET /api/defense-service/admin/statistiques`
    - `GET /api/defense-service/admin/rapports/mensuel`
    - `GET /api/defense-service/admin/rapports/annuel`
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7_

- [ ]\* 18.1 Write unit tests for statistics controller

  - Test statistics endpoint
  - Test report generation endpoints
  - _Requirements: 5.1, 5.6, 5.7_

- [-] 19. Implement Kafka Event Publishing

  - [x] 19.1 Create DefenseEventPublisher service

    - Create `DefenseEventDTO` class
    - Implement `publishDemandeSubmitted(DefenseRequest)` method
    - Implement `publishJuryProposed(Jury)` method
    - Implement `publishRapportSubmitted(Rapport)` method
    - Implement `publishDefenseAuthorized(AutorisationSoutenance)` method
    - Implement `publishDefenseRefused(AutorisationSoutenance)` method
    - Implement `publishDefenseFinalized(Defense)` method
    - Configure KafkaTemplate with JSON serialization
    - _Requirements: 2.7, 2.10, 3.6, 6.1, 6.2, 6.3, 6.7_

  - [ ]\* 19.2 Write property test for event publishing
    - **Property 25: Defense Workflow Event Publishing**
    - **Validates: Requirements 2.7, 2.10, 3.6, 6.1, 6.2, 6.3, 6.7**

- [x] 20. Integrate Event Publishing into Services

  - Update `DefenseRequestService` to publish DEMANDE_SOUTENANCE_SOUMISE event
  - Update `JuryService` to publish JURY_PROPOSE event
  - Update `RapportService` to publish RAPPORT_SOUMIS event
  - Update `AutorisationService` to publish SOUTENANCE_AUTORISEE and SOUTENANCE_REFUSEE events
  - Update `DefenseService` to publish SOUTENANCE_FINALISEE event
  - _Requirements: 2.7, 2.10, 3.6, 6.1, 6.2, 6.3_

- [ ]\* 20.1 Write integration tests for event publishing

  - Test that events are published for each workflow step
  - Verify event payloads contain all required fields
  - _Requirements: 6.1, 6.2, 6.3, 6.7_

- [x] 21. Enhance Resilience4j Configuration

  - [x] 21.1 Update application.yml with circuit breaker configuration

    - Configure circuit breaker for userService
    - Configure retry policy for userService
    - Set thresholds and timeouts
    - _Requirements: 7.1, 7.2_

  - [x] 21.2 Update UserServiceClient with fallback methods

    - Add `@CircuitBreaker` annotation with fallback
    - Add `@Retry` annotation
    - Implement fallback methods returning default user info
    - _Requirements: 7.3, 7.4, 7.5_

  - [ ]\* 21.3 Write property test for circuit breaker activation

    - **Property 26: Circuit Breaker Activation**
    - **Validates: Requirements 7.1, 7.4**

  - [ ]\* 21.4 Write property test for retry policy execution

    - **Property 27: Retry Policy Execution**
    - **Validates: Requirements 7.2, 7.3**

  - [ ]\* 21.5 Write property test for graceful degradation
    - **Property 28: Graceful Degradation**
    - **Validates: Requirements 7.5**

- [x] 22. Implement Configuration Management

  - [x] 22.1 Create DefenseConfigProperties class

    - Add `@ConfigurationProperties` annotation
    - Define properties for min-publications-q1q2, min-conferences, min-heures-formation
    - Define properties for min-membres, min-rapporteurs
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

  - [x] 22.2 Update validation services to use configuration

    - Inject DefenseConfigProperties into PublicationService
    - Inject DefenseConfigProperties into AutorisationService
    - Replace hardcoded values with configuration properties
    - _Requirements: 8.6_

  - [ ]\* 22.3 Write property test for configuration-driven validation
    - **Property 29: Configuration-Driven Validation**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5, 8.6**

- [x] 23. Add Configuration to application.properties

  - Add all defense configuration properties with default values
  - Add PDF generation configuration
  - Add Kafka configuration
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 24. Checkpoint - Ensure all tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [-] 25. Integration Testing

  - [x]\* 25.1 Write integration test for complete defense workflow

    - Test: Submit request → Validate prerequisites → Propose jury → Submit reports → Authorize → Finalize → Generate PV
    - Verify all state transitions and data persistence
    - Verify all Kafka events are published
    - _Requirements: All_

  - [x]\* 25.2 Write integration test for authorization refusal flow

    - Test: Submit request → Verify prerequisites fail → Refuse authorization
    - Verify refusal state and events
    - _Requirements: 2.8, 2.9, 2.10_

  - [x]\* 25.3 Write integration test for resilience patterns
    - Simulate User_Service failures
    - Verify circuit breaker opens
    - Verify retry behavior
    - Verify fallback responses
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 26. Final Checkpoint - Complete Testing and Documentation
  - Run full test suite and verify coverage meets goals (80% line, 75% branch)
  - Generate JaCoCo coverage report
  - Update API documentation with new endpoints
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key milestones
- Property tests validate universal correctness properties across many inputs
- Unit tests validate specific examples and edge cases
- Integration tests verify end-to-end workflows
- The implementation follows Spring Boot best practices with layered architecture
- Resilience4j provides fault tolerance for external service calls
- Kafka enables asynchronous event-driven notifications
- PDF generation uses iText 7 library
- Configuration properties allow external customization without code changes
