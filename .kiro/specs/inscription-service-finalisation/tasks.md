# Implementation Plan

- [x] 1. Set up dependencies and configuration





  - Add ZXing library for QR code generation to pom.xml
  - Add JUnit QuickCheck for property-based testing to pom.xml
  - Update application.properties with new configuration properties for document validation, PDF generation, and alert thresholds
  - Create resource files directory structure for logo and signature images
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 2. Implement document validation service





  - Create InvalidDocumentException class with specific error codes
  - Create DocumentValidationService with MIME type validation logic
  - Implement file size validation method
  - Implement secure file naming pattern generator
  - Add optional ClamAV virus scanning integration
  - _Requirements: 1.1, 1.2, 1.5_

- [ ]* 2.1 Write property test for MIME type validation
  - **Property 1: MIME type validation**
  - **Validates: Requirements 1.1**

- [ ]* 2.2 Write property test for file size validation
  - **Property 2: File size validation**
  - **Validates: Requirements 1.2**

- [ ]* 2.3 Write property test for file naming consistency
  - **Property 3: File naming consistency**
  - **Validates: Requirements 1.5**

- [ ]* 2.4 Write unit tests for document validation edge cases
  - Test exact 10MB boundary
  - Test error messages for invalid MIME type (Requirements 1.3)
  - Test error messages for oversized files (Requirements 1.4)
  - _Requirements: 1.3, 1.4_

- [x] 3. Create new database entities and repositories





  - Create DerogationRequest entity with StatutDerogation enum
  - Create AlerteDuree entity with TypeAlerte enum
  - Create DocumentGenere entity with TypeDocumentGenere enum
  - Add new fields to Inscription entity (bloqueReInscription, relationships)
  - Create DerogationRequestRepository with custom query methods
  - Create AlerteDureeRepository with custom query methods
  - Create DocumentGenereRepository
  - _Requirements: 3.1, 4.1, 2.4, 2.5_

- [x] 4. Implement attestation PDF generation service





  - Create AttestationPdfGenerator service class
  - Implement PDF document structure with iText 7
  - Add institution logo from resources
  - Implement QR code generation with ZXing
  - Add student information section
  - Add thesis details section
  - Add director information section
  - Add signature and stamp section
  - Implement file storage to ./uploads/attestations/
  - Create DocumentGenere record after generation
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 4.1 Write property test for attestation generation trigger
  - **Property 4: Attestation generation trigger**
  - **Validates: Requirements 2.1**

- [ ]* 4.2 Write property test for attestation completeness
  - **Property 5: Attestation completeness**
  - **Validates: Requirements 2.2**

- [ ]* 4.3 Write property test for QR code embedding
  - **Property 6: QR code embedding**
  - **Validates: Requirements 2.3**

- [ ]* 4.4 Write property test for attestation storage
  - **Property 7: Attestation storage**
  - **Validates: Requirements 2.4**

- [ ]* 4.5 Write property test for attestation database record
  - **Property 8: Attestation database record**
  - **Validates: Requirements 2.5**

- [x] 5. Add attestation download endpoint


  - Create GET /api/inscriptions/{id}/attestation endpoint in InscriptionController
  - Implement authorization check (student, director, or admin)
  - Return PDF file with correct Content-Type header
  - Handle file not found scenarios
  - _Requirements: 2.6, 2.7_

- [ ]* 5.1 Write property test for attestation download authorization
  - **Property 9: Attestation download authorization**
  - **Validates: Requirements 2.6**

- [ ]* 5.2 Write property test for attestation content type
  - **Property 10: Attestation content type**
  - **Validates: Requirements 2.7**

- [x] 6. Integrate attestation generation into validation workflow



  - Update InscriptionService.validerParAdmin method
  - Call attestation generator when status changes to VALIDE
  - Handle attestation generation errors gracefully
  - _Requirements: 2.1_

- [x] 7. Implement derogation service






  - Create DerogationService class
  - Implement creerDerogation method with validation
  - Implement validerParDirecteur method with state transitions
  - Implement validerParPED method with final approval logic
  - Implement getDerogation query method
  - Implement isDerogationRequise helper method
  - Create DerogationRequiredException class
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [ ]* 7.1 Write property test for derogation creation
  - **Property 11: Derogation creation**
  - **Validates: Requirements 3.1**

- [ ]* 7.2 Write property test for derogation notification to director
  - **Property 12: Derogation notification to director**
  - **Validates: Requirements 3.2**

- [ ]* 7.3 Write property test for director approval workflow
  - **Property 13: Director approval workflow**
  - **Validates: Requirements 3.3**

- [ ]* 7.4 Write property test for director rejection workflow
  - **Property 14: Director rejection workflow**
  - **Validates: Requirements 3.4**

- [ ]* 7.5 Write property test for PED approval workflow
  - **Property 15: PED approval workflow**
  - **Validates: Requirements 3.5**

- [ ]* 7.6 Write property test for PED rejection workflow
  - **Property 16: PED rejection workflow**
  - **Validates: Requirements 3.6**


- [x] 8. Add derogation REST endpoints


  - Create POST /api/inscriptions/{id}/derogation endpoint
  - Create POST /api/inscriptions/{id}/derogation/valider-directeur endpoint
  - Create POST /api/inscriptions/{id}/derogation/valider-ped endpoint
  - Create GET /api/inscriptions/{id}/derogation endpoint
  - Add request/response DTOs for derogation operations
  - Add authorization checks for each endpoint
  - _Requirements: 3.1, 3.3, 3.4, 3.5, 3.6_
-

- [x] 9. Integrate derogation validation into re-registration workflow





  - Update InscriptionService.creerInscription method
  - Check if duration > 3 years and derogation is required
  - Verify approved derogation exists before allowing re-registration
  - Throw DerogationRequiredException if missing
  - _Requirements: 3.7_

- [ ]* 9.1 Write property test for re-registration derogation requirement
  - **Property 17: Re-registration derogation requirement**
  - **Validates: Requirements 3.7**

- [x] 10. Checkpoint - Ensure all tests pass










  - Ensure all tests pass, ask the user if questions arise.


- [x] 11. Implement alert service





  - Create AlerteService class
  - Implement verifierEtGenererAlertes method with duration calculation
  - Implement alert threshold checking (2.5, 5.5, 6.0 years)
  - Implement alerteExiste method to prevent duplicates
  - Implement creerAlerte method with Kafka notification
  - Implement getAlertesActives query method
  - Add logic to set bloqueReInscription flag at 6 years
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 11.1 Write property test for 3-year approach alert
  - **Property 18: 3-year approach alert**
  - **Validates: Requirements 4.1**

- [ ]* 11.2 Write property test for 6-year approach alert
  - **Property 19: 6-year approach alert**
  - **Validates: Requirements 4.2**

- [ ]* 11.3 Write property test for 6-year exceeded alert and blocking
  - **Property 20: 6-year exceeded alert and blocking**
  - **Validates: Requirements 4.3**

- [ ]* 11.4 Write property test for alert notification publishing
  - **Property 21: Alert notification publishing**
  - **Validates: Requirements 4.4**

- [ ]* 11.5 Write property test for alert idempotency
  - **Property 22: Alert idempotency**
  - **Validates: Requirements 4.5**

- [x] 12. Integrate alert verification into re-registration workflow











  - Update InscriptionService.creerInscription to call alert verification
  - Update InscriptionService.soumettre to call alert verification
  - Ensure alerts are checked before allowing re-registration
  - _Requirements: 4.6_

- [ ]* 12.1 Write property test for re-registration alert verification
  - **Property 23: Re-registration alert verification**
  - **Validates: Requirements 4.6**

- [x] 13. Create alert verification endpoint for batch service





  - Create GET /api/inscriptions/verifier-alertes endpoint
  - Implement batch processing for all active inscriptions
  - Return summary of alerts generated
  - Add appropriate authorization for batch service
  - _Requirements: 4.1, 4.2, 4.3_

- [x] 14. Implement dashboard service





  - Create DashboardService class
  - Implement getDashboardDoctorant method
  - Fetch current inscription with all details
  - Fetch inscription history ordered by year
  - Fetch active alerts for the student
  - Implement getDocumentsManquants method
  - Implement calculerStatistiques method
  - Implement getProchaineMilestone method
  - Create DashboardResponse DTO with all nested structures
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [ ]* 14.1 Write property test for dashboard current inscription
  - **Property 24: Dashboard current inscription**
  - **Validates: Requirements 5.1**

- [ ]* 14.2 Write property test for dashboard inscription history
  - **Property 25: Dashboard inscription history**
  - **Validates: Requirements 5.2**

- [ ]* 14.3 Write property test for dashboard active alerts
  - **Property 26: Dashboard active alerts**
  - **Validates: Requirements 5.3**

- [ ]* 14.4 Write property test for dashboard missing documents
  - **Property 27: Dashboard missing documents**
  - **Validates: Requirements 5.4**

- [ ]* 14.5 Write property test for dashboard statistics calculation
  - **Property 28: Dashboard statistics calculation**
  - **Validates: Requirements 5.5**

- [ ]* 14.6 Write property test for dashboard milestone calculation
  - **Property 29: Dashboard milestone calculation**
  - **Validates: Requirements 5.6**

- [x] 15. Add dashboard REST endpoint





  - Create GET /api/inscriptions/doctorant/{id}/dashboard endpoint in InscriptionController
  - Add authorization check for student, director, or admin
  - Return complete dashboard response
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [x] 16. Enhance campaign service with scheduled tasks





  - Update CampagneService class
  - Add @Scheduled method verifierCampagnes with cron expression "0 0 8 * * ?"
  - Implement campaign opening logic with notifications
  - Implement campaign closing logic with notifications and flag update
  - Add Kafka event publishing for campaign lifecycle
  - _Requirements: 6.1, 6.2, 6.5_

- [ ]* 16.1 Write property test for campaign opening notification
  - **Property 30: Campaign opening notification**
  - **Validates: Requirements 6.1**

- [ ]* 16.2 Write property test for campaign closing automation
  - **Property 31: Campaign closing automation**
  - **Validates: Requirements 6.2**

- [x] 17. Implement campaign statistics




  - Add getStatistiques method to CampagneService
  - Calculate total inscriptions for campaign
  - Calculate breakdown by status
  - Calculate validation rate percentage
  - Calculate average validation time in days
  - Create StatistiquesCampagne DTO
  - _Requirements: 6.3_

- [ ]* 17.1 Write property test for campaign statistics accuracy
  - **Property 32: Campaign statistics accuracy**
  - **Validates: Requirements 6.3**

- [x] 18. Implement campaign cloning





  - Add clonerCampagne method to CampagneService
  - Copy campaign type and libelle
  - Increment year in libelle
  - Set new dates from parameters
  - Set active flag to false initially
  - _Requirements: 6.4_

- [ ]* 18.1 Write property test for campaign cloning
  - **Property 33: Campaign cloning**
  - **Validates: Requirements 6.4**

- [x] 19. Add campaign management endpoints






  - Create GET /api/campagnes/{id}/statistiques endpoint in CampagneController
  - Create POST /api/campagnes/{id}/cloner endpoint in CampagneController
  - Add authorization checks for admin role
  - _Requirements: 6.3, 6.4_

- [x] 20. Enhance Kafka event publishing








  - Update NotificationService to publish all required events
  - Add INSCRIPTION_SOUMISE event publishing
  - Add INSCRIPTION_VALIDEE_DIRECTEUR event publishing
  - Add INSCRIPTION_REJETEE_DIRECTEUR event publishing
  - Add INSCRIPTION_VALIDEE_ADMIN event publishing
  - Add INSCRIPTION_REJETEE_ADMIN event publishing
  - Add CAMPAGNE_OUVERTE event publishing
  - Add CAMPAGNE_FERMEE event publishing
  - Add DEROGATION_DEMANDEE event publishing
  - Add ALERTE_DUREE event publishing
  - Ensure all events include required data fields
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9_

- [ ]* 20.1 Write property test for inscription submission event
  - **Property 34: Inscription submission event**
  - **Validates: Requirements 7.1**

- [ ]* 20.2 Write property test for director validation event
  - **Property 35: Director validation event**
  - **Validates: Requirements 7.2**

- [ ]* 20.3 Write property test for director rejection event
  - **Property 36: Director rejection event**
  - **Validates: Requirements 7.3**

- [ ]* 20.4 Write property test for admin validation event
  - **Property 37: Admin validation event**
  - **Validates: Requirements 7.4**

- [ ]* 20.5 Write property test for admin rejection event
  - **Property 38: Admin rejection event**
  - **Validates: Requirements 7.5**

- [ ]* 20.6 Write property test for campaign opened event
  - **Property 39: Campaign opened event**
  - **Validates: Requirements 7.6**

- [ ]* 20.7 Write property test for campaign closed event
  - **Property 40: Campaign closed event**
  - **Validates: Requirements 7.7**

- [ ]* 20.8 Write property test for derogation requested event
  - **Property 41: Derogation requested event**
  - **Validates: Requirements 7.8**

- [ ]* 20.9 Write property test for alert duration event
  - **Property 42: Alert duration event**
  - **Validates: Requirements 7.9**

- [x] 21. Update document controller to use validation service





  - Modify DocumentController upload endpoint
  - Call DocumentValidationService before saving document
  - Use secure file naming from validation service
  - Handle InvalidDocumentException appropriately
  - _Requirements: 1.1, 1.2, 1.5_
-

- [x] 22. Create global exception handler





  - Create GlobalExceptionHandler class with @RestControllerAdvice
  - Add handler for InvalidDocumentException
  - Add handler for DerogationRequiredException
  - Add handler for DurationLimitExceededException
  - Add handler for UnauthorizedAccessException
  - Add handler for CampagneClosedException
  - Add generic exception handler
  - Ensure all error responses follow standard format
  - _Requirements: 1.3, 1.4, 3.7, 4.3_

- [ ]* 22.1 Write unit tests for exception handlers
  - Test error response format
  - Test HTTP status codes
  - Test error messages

- [x] 23. Add Feign Client integration for user information





  - Update UserServiceClient interface
  - Add method to fetch director information for attestation
  - Add method to fetch student information for dashboard
  - Configure circuit breaker and retry for resilience
  - _Requirements: 2.2, 5.1_

- [ ]* 23.1 Write integration tests for Feign Client
  - Test successful user fetch
  - Test circuit breaker activation
  - Test retry mechanism

- [x] 24. Create custom generators for property-based tests





  - Create InscriptionGenerator for random inscriptions
  - Create DocumentGenerator for random multipart files
  - Create InvalidMimeTypeGenerator for testing validation
  - Create DerogationRequestGenerator for derogation tests
  - Create CampagneGenerator for campaign tests
  - Ensure generators produce valid and edge-case data
  - _Requirements: All property tests_

- [x] 25. Write integration tests for complete workflows





  - Write test for complete inscription workflow (create → submit → validate → attestation)
  - Write test for derogation workflow (request → director approve → PED approve)
  - Write test for campaign lifecycle (open → inscriptions → close)
  - Write test for alert generation workflow
  - Use TestContainers for database
  - Use embedded Kafka for messaging
  - _Requirements: All requirements_

- [x] 26. Final checkpoint - Ensure all tests pass





  - Ensure all tests pass, ask the user if questions arise.
  - Verify all 42 correctness properties are tested
  - Verify test coverage meets goals (>80% unit, 100% critical paths)
  - Run full integration test suite
