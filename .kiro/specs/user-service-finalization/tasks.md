# Implementation Plan: User Service Finalization

## Overview

This implementation plan breaks down the user-service finalization into discrete, incremental tasks. Each task builds on previous work and includes testing to validate functionality. The plan follows a logical progression: data models → services → controllers → integration → testing.

## Tasks

- [x] 1. Set up project dependencies and configuration

  - Add Kafka dependencies to pom.xml (spring-kafka)
  - Add property-based testing dependencies (jqwik)
  - Add TestContainers dependencies (mariadb, kafka)
  - Update application.yml with user-service configuration section
  - Add Kafka configuration properties
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [x] 2. Create enums and base entities

  - [x] 2.1 Create AccountStatus enum

    - Define ACTIVE, DISABLED, LOCKED values
    - _Requirements: 3.1, 3.4, 4.3_

  - [x] 2.2 Create AuditAction enum

    - Define all audit action types (LOGIN, LOGIN_FAILED, PASSWORD_CHANGE, etc.)
    - _Requirements: 2.8_

  - [x] 2.3 Update User entity
    - Add accountStatus field with default ACTIVE
    - Add failedLoginAttempts field with default 0
    - Add lockoutExpiration field
    - Add OneToOne relationship to UserProfile
    - _Requirements: 3.1, 4.1, 4.3, 8.7_

- [x] 3. Implement UserProfile entity and repository

  - [x] 3.1 Create UserProfile entity

    - Define fields: dateNaissance, lieuNaissance, nationalite, cin, photoUrl
    - Add OneToOne relationship to User
    - Add unique constraint on cin
    - Add timestamps (createdAt, updatedAt)
    - _Requirements: 1.2, 8.1_

  - [x] 3.2 Create UserProfileRepository

    - Add findByUserId method
    - Add existsByCin method
    - _Requirements: 1.1, 1.6_

  - [ ]\* 3.3 Write property test for UserProfile
    - **Property 2: CIN Uniqueness**
    - **Validates: Requirements 1.6**

- [x] 4. Implement UserAudit entity and repository

  - [x] 4.1 Create UserAudit entity

    - Define fields: userId, action, timestamp, ipAddress, details
    - Add indexes on userId, action, timestamp
    - _Requirements: 2.8, 8.2_

  - [x] 4.2 Create UserAuditRepository

    - Add findByUserIdOrderByTimestampDesc with pagination
    - Add findByUserIdAndActionOrderByTimestampDesc with pagination
    - Add findByTimestampAfter method
    - Add countByActionAndTimestampBetween method
    - Add findByActionAndTimestampBetween method
    - _Requirements: 2.6, 2.7, 8.4_

  - [ ]\* 4.3 Write unit tests for UserAuditRepository
    - Test pagination and filtering
    - Test date range queries
    - _Requirements: 2.6, 2.7_

- [x] 5. Implement Kafka event publishing

  - [x] 5.1 Create UserEvent record

    - Define fields: eventType, userId, email, timestamp, metadata
    - _Requirements: 6.6_

  - [x] 5.2 Create KafkaProducerConfig

    - Configure ProducerFactory with JsonSerializer
    - Create KafkaTemplate bean
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [x] 5.3 Create UserEventPublisher service

    - Implement publishUserRegistered method
    - Implement publishUserDisabled method
    - Implement publishUserEnabled method
    - Implement publishPasswordChanged method
    - Implement publishRoleAssigned method
    - Add error handling with logging (non-blocking)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.7_

  - [ ]\* 5.4 Write property test for event publishing
    - **Property 11: Event Publishing Non-Blocking**
    - **Validates: Requirements 6.7**

- [x] 6. Implement AuditService

  - [x] 6.1 Create AuditService

    - Implement logLogin method (success and failure)
    - Implement logPasswordChange method
    - Implement logRoleAssigned method
    - Implement logProfileModified method
    - Implement logAccountDisabled method
    - Implement logAccountEnabled method
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.8_

  - [x] 6.2 Add audit query methods

    - Implement getUserAuditHistory with pagination and filtering
    - Implement getRecentAudits (last 24 hours)
    - _Requirements: 2.6, 2.7_

  - [ ]\* 6.3 Write property test for AuditService

    - **Property 3: Audit Trail Completeness**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

  - [ ]\* 6.4 Write unit tests for AuditService
    - Test each log method creates correct audit record
    - Test pagination and filtering
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

- [x] 7. Implement LoginAttemptService

  - [x] 7.1 Create LoginAttemptService

    - Inject configuration properties (max-failed-logins, lockout-duration)
    - Implement loginSucceeded method (reset counter)
    - Implement loginFailed method (increment counter, lock if needed)
    - Implement isAccountLocked method (check and auto-unlock)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 9.1, 9.2_

  - [ ]\* 7.2 Write property test for failed login counter reset

    - **Property 7: Failed Login Counter Reset**
    - **Validates: Requirements 4.6**

  - [ ]\* 7.3 Write property test for account locking

    - **Property 8: Account Locking After Max Attempts**
    - **Validates: Requirements 4.2, 4.3**

  - [ ]\* 7.4 Write property test for automatic unlock

    - **Property 9: Automatic Unlock After Expiration**
    - **Validates: Requirements 4.5**

  - [ ]\* 7.5 Write unit tests for LoginAttemptService
    - Test loginSucceeded resets counter
    - Test loginFailed increments counter
    - Test account locks after max attempts
    - Test auto-unlock after expiration
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [x] 8. Create utility classes

  - [x] 8.1 Create IpAddressExtractor component

    - Implement extractIpAddress method
    - Handle X-Forwarded-For header
    - Fallback to RemoteAddr
    - _Requirements: 2.8_

  - [ ]\* 8.2 Write unit tests for IpAddressExtractor
    - Test X-Forwarded-For extraction
    - Test fallback to RemoteAddr
    - _Requirements: 2.8_

- [x] 9. Update AuthService with audit and login protection

  - [x] 9.1 Inject AuditService and LoginAttemptService

    - Add dependencies to AuthService
    - _Requirements: 2.1, 4.1_

  - [x] 9.2 Update login method

    - Add ipAddress parameter
    - Check if account is locked before authentication
    - Check if account is disabled before authentication
    - Log failed login attempts
    - Call loginSucceeded on successful login
    - Call loginFailed on failed login
    - Log successful login audit
    - _Requirements: 2.1, 2.2, 3.3, 4.1, 4.2, 4.3, 4.4_

  - [x] 9.3 Update register method

    - Publish USER_REGISTERED event after successful registration
    - _Requirements: 6.1_

  - [ ]\* 9.4 Write property test for account disable prevents login

    - **Property 4: Account Disable Prevents Login**
    - **Validates: Requirements 3.3**

  - [ ]\* 9.5 Write unit tests for updated AuthService
    - Test login with locked account throws exception
    - Test login with disabled account throws exception
    - Test successful login logs audit
    - Test failed login logs audit and increments counter
    - Test register publishes event
    - _Requirements: 2.1, 2.2, 3.3, 4.3, 4.4, 6.1_

- [x] 10. Checkpoint - Ensure authentication and audit tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Extend UserService with profile management

  - [x] 11.1 Create ProfileCompleteRequest DTO

    - Add validation annotations
    - Add CIN pattern validation
    - _Requirements: 1.1, 1.6_

  - [x] 11.2 Create UserDetailedResponse and ProfileData DTOs

    - Include profileComplete flag
    - Include profile data
    - Include account status
    - _Requirements: 1.3, 1.5_

  - [x] 11.3 Implement completeProfile method

    - Validate user exists
    - Check CIN uniqueness
    - Create or update UserProfile
    - Set profileComplete flag
    - Log profile modified audit
    - Publish event
    - Return detailed response
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6_

  - [x] 11.4 Implement getDetailedProfile method

    - Fetch user with profile
    - Check profileComplete status
    - Return detailed response
    - _Requirements: 1.3, 1.5_

  - [ ]\* 11.5 Write property test for profile completion idempotence

    - **Property 1: Profile Completion Idempotence**
    - **Validates: Requirements 1.1, 1.4**

  - [ ]\* 11.6 Write unit tests for profile management
    - Test completeProfile with valid data
    - Test completeProfile with duplicate CIN throws exception
    - Test getDetailedProfile returns correct data
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [x] 12. Extend UserService with account management

  - [x] 12.1 Create DisableAccountRequest DTO

    - Add reason field with validation
    - _Requirements: 3.1_

  - [x] 12.2 Implement disableAccount method

    - Validate admin cannot disable self
    - Set account status to DISABLED
    - Invalidate all refresh tokens
    - Log account disabled audit
    - Publish USER_DISABLED event
    - _Requirements: 3.1, 3.2, 3.6, 3.8_

  - [x] 12.3 Implement enableAccount method

    - Set account status to ACTIVE
    - Reset failed login attempts
    - Log account enabled audit
    - Publish USER_ENABLED event
    - _Requirements: 3.4, 3.5_

  - [x] 12.4 Implement getDisabledAccounts method

    - Query disabled accounts with pagination
    - Map to response DTOs
    - _Requirements: 3.7_

  - [ ]\* 12.5 Write property test for self-disable prevention

    - **Property 5: Self-Disable Prevention**
    - **Validates: Requirements 3.6**

  - [ ]\* 12.6 Write property test for token invalidation

    - **Property 6: Token Invalidation on Disable**
    - **Validates: Requirements 3.8**

  - [ ]\* 12.7 Write unit tests for account management
    - Test disableAccount sets status and invalidates tokens
    - Test disableAccount with self-disable throws exception
    - Test enableAccount sets status and resets counter
    - Test getDisabledAccounts returns correct results
    - _Requirements: 3.1, 3.2, 3.4, 3.5, 3.6, 3.7, 3.8_

- [x] 13. Implement StatisticsService

  - [x] 13.1 Create statistics response DTOs

    - Create UserStatisticsResponse
    - Create ConnectionStatisticsResponse
    - Create DailyConnectionCount
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 13.2 Implement getUserStatistics method

    - Count total users
    - Count by role
    - Count by account status
    - Count new users this month
    - _Requirements: 5.1, 5.5_

  - [x] 13.3 Implement getConnectionStatistics method

    - Query LOGIN audits for last 30 days
    - Group by date
    - Group by role
    - _Requirements: 5.2, 5.3, 5.4_

  - [ ]\* 13.4 Write property test for statistics accuracy

    - **Property 10: Statistics Accuracy**
    - **Validates: Requirements 5.1, 5.5**

  - [ ]\* 13.5 Write unit tests for StatisticsService
    - Test getUserStatistics returns accurate counts
    - Test getConnectionStatistics returns correct data
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 14. Implement UserController endpoints

  - [x] 14.1 Add completeProfile endpoint

    - PUT /api/users/profile/complete
    - Extract user ID from authentication
    - Call userService.completeProfile
    - Return 200 OK with detailed response
    - _Requirements: 7.1, 10.1_

  - [x] 14.2 Add getDetailedProfile endpoint

    - GET /api/users/{id}/profile-complete
    - Check authorization (own profile or admin)
    - Call userService.getDetailedProfile
    - Return 200 OK with detailed response
    - _Requirements: 7.2, 10.2, 10.3_

  - [ ]\* 14.3 Write property test for profile access authorization

    - **Property 13: Profile Access Authorization**
    - **Validates: Requirements 10.3**

  - [ ]\* 14.4 Write unit tests for UserController
    - Test completeProfile endpoint
    - Test getDetailedProfile with own profile
    - Test getDetailedProfile with admin access
    - Test getDetailedProfile unauthorized returns 403
    - _Requirements: 7.1, 7.2, 10.1, 10.2, 10.3_

- [x] 15. Implement AdminController endpoints

  - [x] 15.1 Create AdminController class

    - Add @PreAuthorize("hasRole('ADMIN')") annotation
    - _Requirements: 7.10, 10.4_

  - [x] 15.2 Add disableUser endpoint

    - POST /api/admin/users/{userId}/disable
    - Extract admin ID from authentication
    - Call userService.disableAccount
    - Return 204 No Content
    - _Requirements: 7.5_

  - [x] 15.3 Add enableUser endpoint

    - POST /api/admin/users/{userId}/enable
    - Extract admin ID from authentication
    - Call userService.enableAccount
    - Return 204 No Content
    - _Requirements: 7.6_

  - [x] 15.4 Add getDisabledUsers endpoint

    - GET /api/admin/users/disabled
    - Call userService.getDisabledAccounts
    - Return 200 OK with paginated results
    - _Requirements: 7.7_

  - [x] 15.5 Add getUserStatistics endpoint

    - GET /api/admin/statistics/users
    - Call statisticsService.getUserStatistics
    - Return 200 OK with statistics
    - _Requirements: 7.8_

  - [x] 15.6 Add getConnectionStatistics endpoint

    - GET /api/admin/statistics/connections
    - Call statisticsService.getConnectionStatistics
    - Return 200 OK with connection stats
    - _Requirements: 7.9_

  - [ ]\* 15.7 Write property test for admin authorization

    - **Property 12: Admin Authorization Enforcement**
    - **Validates: Requirements 7.10, 10.4**

  - [ ]\* 15.8 Write unit tests for AdminController
    - Test all endpoints with admin user
    - Test all endpoints with non-admin user return 403
    - _Requirements: 7.5, 7.6, 7.7, 7.8, 7.9, 7.10, 10.4_

- [x] 16. Implement AuditController endpoints

  - [x] 16.1 Create AuditController class

    - Add @PreAuthorize("hasRole('ADMIN')") annotation
    - _Requirements: 10.4_

  - [x] 16.2 Add getUserAuditHistory endpoint

    - GET /api/admin/audit/users/{userId}
    - Support optional action filter
    - Call auditService.getUserAuditHistory
    - Return 200 OK with paginated audit records
    - _Requirements: 7.3_

  - [x] 16.3 Add getRecentAudits endpoint

    - GET /api/admin/audit/recent
    - Call auditService.getRecentAudits
    - Return 200 OK with recent audits
    - _Requirements: 7.4_

  - [ ]\* 16.4 Write unit tests for AuditController
    - Test getUserAuditHistory with and without filter
    - Test getRecentAudits
    - Test non-admin access returns 403
    - _Requirements: 7.3, 7.4, 10.4_

- [x] 17. Implement exception handling

  - [x] 17.1 Create custom exceptions

    - Create AccountLockedException
    - Create AccountDisabledException
    - Create SelfDisableException
    - Create DuplicateCinException
    - _Requirements: 3.3, 3.6, 4.4, 1.6_

  - [x] 17.2 Update GlobalExceptionHandler

    - Add handler for AccountLockedException (403)
    - Add handler for AccountDisabledException (403)
    - Add handler for SelfDisableException (400)
    - Add handler for DuplicateCinException (409)
    - _Requirements: 7.11_

  - [ ]\* 17.3 Write unit tests for exception handlers
    - Test each exception returns correct status code
    - Test error response format
    - _Requirements: 7.11_

- [x] 18. Update AuthController to pass IP address

  - [x] 18.1 Inject IpAddressExtractor

    - Add dependency to AuthController
    - _Requirements: 2.8_

  - [x] 18.2 Update login endpoint

    - Extract IP address from request
    - Pass IP address to authService.login
    - _Requirements: 2.1, 2.2_

  - [ ]\* 18.3 Write unit tests for updated AuthController
    - Test login extracts and passes IP address
    - _Requirements: 2.1, 2.2_

- [x] 19. Checkpoint - Ensure all unit and property tests pass

  - Ensure all tests pass, ask the user if questions arise.

- [x] 20. Write integration tests

  - [ ]\* 20.1 Set up TestContainers configuration

    - Configure MariaDB container
    - Configure Kafka container
    - Create base integration test class

  - [ ]\* 20.2 Write complete authentication flow integration test

    - Test register → login → access protected resource → refresh token
    - Verify audit records created
    - Verify events published
    - _Requirements: 2.1, 6.1_

  - [ ]\* 20.3 Write profile completion integration test

    - Test complete profile → get detailed profile
    - Verify profile data persisted
    - Verify audit record created
    - _Requirements: 1.1, 1.2, 1.3, 2.5_

  - [ ]\* 20.4 Write account disable integration test

    - Test disable account → verify tokens invalidated → attempt login
    - Verify audit record created
    - Verify event published
    - _Requirements: 3.1, 3.2, 3.3, 3.8, 6.2_

  - [ ]\* 20.5 Write failed login protection integration test

    - Test multiple failed logins → account locked → auto-unlock
    - Verify audit records created
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ]\* 20.6 Write admin statistics integration test
    - Create test data → query statistics
    - Verify accuracy of counts
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 21. Final checkpoint - Run full test suite
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end flows
