# Requirements Document

## Introduction

This document specifies the requirements for finalizing the user-service microservice. The service currently handles JWT authentication, user management, and password reset at 90% completion. This finalization adds detailed profile management, audit trails, account activation/deactivation, admin statistics, and enhanced security features to bring the service to production readiness.

## Glossary

- **User_Service**: The microservice responsible for user authentication, authorization, and profile management
- **JWT**: JSON Web Token used for stateless authentication
- **Audit_Trail**: Historical record of all security-sensitive actions performed by users
- **User_Profile**: Extended user information beyond basic authentication data
- **Account_Status**: The state of a user account (active, disabled, locked)
- **Admin_User**: A user with ROLE_ADMIN privileges
- **Doctorant**: A doctoral student user with ROLE_DOCTORANT
- **Directeur**: A thesis director with ROLE_DIRECTEUR
- **Access_Token**: Short-lived JWT token for API authentication
- **Refresh_Token**: Long-lived token used to obtain new access tokens
- **Kafka_Event**: Asynchronous message published to Kafka topics for inter-service communication
- **Profile_Completion**: The process of adding detailed personal information to a user profile

## Requirements

### Requirement 1: Detailed Profile Management

**User Story:** As a doctorant, I want to complete my profile with detailed personal information, so that the system has all necessary data for administrative processes.

#### Acceptance Criteria

1. WHEN a user submits complete profile information, THE User_Service SHALL validate all required fields and persist the data
2. WHEN profile data includes dateNaissance, lieuNaissance, nationalite, cin, and photoUrl, THE User_Service SHALL store these in a UserProfile entity linked to the User
3. WHEN a user requests their complete profile, THE User_Service SHALL return all basic and extended profile information
4. WHEN profile completion is successful, THE User_Service SHALL set profileComplete flag to true
5. WHEN a user with incomplete profile is retrieved, THE User_Service SHALL indicate profileComplete as false
6. WHEN invalid data is submitted (empty required fields, invalid date format, invalid CIN format), THE User_Service SHALL reject the request with descriptive error messages

### Requirement 2: Audit Trail and History

**User Story:** As an admin, I want to track all security-sensitive actions, so that I can monitor system security and investigate incidents.

#### Acceptance Criteria

1. WHEN a user successfully logs in, THE User_Service SHALL create an audit record with action LOGIN, timestamp, and IP address
2. WHEN a login attempt fails, THE User_Service SHALL create an audit record with action LOGIN_FAILED and IP address
3. WHEN a user changes their password, THE User_Service SHALL create an audit record with action PASSWORD_CHANGE
4. WHEN a role is assigned to a user, THE User_Service SHALL create an audit record with action ROLE_ASSIGNED and role details
5. WHEN a user profile is modified, THE User_Service SHALL create an audit record with action PROFILE_MODIFIED
6. WHEN an admin requests audit history for a user, THE User_Service SHALL return paginated audit records filtered by userId
7. WHEN an admin requests recent audit events, THE User_Service SHALL return all audit records from the last 24 hours
8. WHEN audit records are created, THE User_Service SHALL include userId, action type, timestamp, IP address, and action details

### Requirement 3: Account Activation and Deactivation

**User Story:** As an admin, I want to disable and enable user accounts, so that I can manage access control and handle policy violations.

#### Acceptance Criteria

1. WHEN an admin disables a user account with a reason, THE User_Service SHALL set the account status to disabled and store the reason
2. WHEN a user account is disabled, THE User_Service SHALL publish a USER_DISABLED event to Kafka
3. WHEN a disabled user attempts to login, THE User_Service SHALL reject the authentication and return an account disabled error
4. WHEN an admin enables a previously disabled account, THE User_Service SHALL set the account status to active
5. WHEN a user account is enabled, THE User_Service SHALL publish a USER_ENABLED event to Kafka
6. WHEN an admin attempts to disable their own account, THE User_Service SHALL reject the request
7. WHEN an admin requests the list of disabled accounts, THE User_Service SHALL return paginated results of all disabled users
8. WHEN a user account is disabled, THE User_Service SHALL invalidate all existing tokens for that user

### Requirement 4: Failed Login Protection

**User Story:** As a system administrator, I want accounts to be temporarily locked after repeated failed login attempts, so that brute force attacks are prevented.

#### Acceptance Criteria

1. WHEN a user fails to login, THE User_Service SHALL increment the failed login counter for that account
2. WHEN the failed login counter reaches the configured maximum (5 attempts), THE User_Service SHALL lock the account temporarily
3. WHEN an account is locked due to failed attempts, THE User_Service SHALL set a lockout expiration time based on configured duration (30 minutes)
4. WHEN a locked user attempts to login before lockout expiration, THE User_Service SHALL reject authentication with account locked error
5. WHEN the lockout duration expires, THE User_Service SHALL automatically unlock the account and reset the failed login counter
6. WHEN a user successfully logs in, THE User_Service SHALL reset the failed login counter to zero

### Requirement 5: Admin Statistics and Dashboard

**User Story:** As an admin, I want to view user statistics and connection metrics, so that I can monitor system usage and user activity.

#### Acceptance Criteria

1. WHEN an admin requests user statistics, THE User_Service SHALL return total user count, count by role, active count, disabled count, and new users this month
2. WHEN an admin requests connection statistics, THE User_Service SHALL return daily connection counts for the last 30 days
3. WHEN connection statistics are requested, THE User_Service SHALL include breakdown by user role
4. WHEN statistics are calculated, THE User_Service SHALL use audit trail data for connection metrics
5. WHEN user counts are calculated, THE User_Service SHALL query current database state for accurate totals

### Requirement 6: Event Publishing for Inter-Service Communication

**User Story:** As a system architect, I want user-related events published to Kafka, so that other services can react to user changes asynchronously.

#### Acceptance Criteria

1. WHEN a new user registers, THE User_Service SHALL publish a USER_REGISTERED event to the user-events topic
2. WHEN a user account is disabled, THE User_Service SHALL publish a USER_DISABLED event with userId and reason
3. WHEN a user account is enabled, THE User_Service SHALL publish a USER_ENABLED event with userId
4. WHEN a user changes their password, THE User_Service SHALL publish a PASSWORD_CHANGED event with userId
5. WHEN a role is assigned to a user, THE User_Service SHALL publish a ROLE_ASSIGNED event with userId and role name
6. WHEN events are published, THE User_Service SHALL include timestamp and event type in the message payload
7. WHEN event publishing fails, THE User_Service SHALL log the error but not fail the primary operation

### Requirement 7: API Endpoints Implementation

**User Story:** As a developer, I want well-defined REST endpoints for all user management operations, so that I can integrate with the user service.

#### Acceptance Criteria

1. THE User_Service SHALL expose PUT /api/users/profile/complete endpoint accepting profile completion data
2. THE User_Service SHALL expose GET /api/users/{id}/profile-complete endpoint returning detailed user profile
3. THE User_Service SHALL expose GET /api/admin/audit/users/{userId} endpoint returning paginated audit history
4. THE User_Service SHALL expose GET /api/admin/audit/recent endpoint returning recent audit events
5. THE User_Service SHALL expose POST /api/admin/users/{userId}/disable endpoint accepting disable reason
6. THE User_Service SHALL expose POST /api/admin/users/{userId}/enable endpoint for account reactivation
7. THE User_Service SHALL expose GET /api/admin/users/disabled endpoint returning paginated disabled accounts
8. THE User_Service SHALL expose GET /api/admin/statistics/users endpoint returning user statistics
9. THE User_Service SHALL expose GET /api/admin/statistics/connections endpoint returning connection metrics
10. WHEN admin endpoints are accessed by non-admin users, THE User_Service SHALL return 403 Forbidden
11. WHEN endpoints receive invalid data, THE User_Service SHALL return 400 Bad Request with validation errors

### Requirement 8: Data Persistence and Relationships

**User Story:** As a developer, I want proper data models and relationships, so that user data is stored consistently and efficiently.

#### Acceptance Criteria

1. THE User_Service SHALL define a UserProfile entity with OneToOne relationship to User
2. THE User_Service SHALL define a UserAudit entity with fields: id, userId, action, timestamp, ipAddress, details
3. WHEN a User is deleted, THE User_Service SHALL cascade delete the associated UserProfile
4. WHEN audit records are queried, THE User_Service SHALL support filtering by userId, action type, and date range
5. WHEN user data is persisted, THE User_Service SHALL enforce database constraints for required fields
6. THE User_Service SHALL add accountStatus field to User entity with values: ACTIVE, DISABLED, LOCKED
7. THE User_Service SHALL add failedLoginAttempts and lockoutExpiration fields to User entity

### Requirement 9: Configuration Management

**User Story:** As a system administrator, I want configurable security parameters, so that I can adjust security policies without code changes.

#### Acceptance Criteria

1. THE User_Service SHALL read max-failed-logins configuration from application.yml
2. THE User_Service SHALL read lockout-duration configuration from application.yml
3. THE User_Service SHALL read audit retention-days configuration from application.yml
4. WHEN configuration values are missing, THE User_Service SHALL use sensible defaults (5 attempts, 30 minutes, 365 days)
5. WHEN audit retention period is exceeded, THE User_Service SHALL provide mechanism to archive or delete old records

### Requirement 10: Security and Authorization

**User Story:** As a security engineer, I want proper authorization controls on all endpoints, so that users can only access permitted operations.

#### Acceptance Criteria

1. WHEN a user accesses profile completion endpoint, THE User_Service SHALL verify the user is authenticated
2. WHEN a user accesses their own profile, THE User_Service SHALL allow the operation
3. WHEN a user attempts to access another user's profile, THE User_Service SHALL allow only if requester is admin
4. WHEN any admin endpoint is accessed, THE User_Service SHALL verify the requester has ROLE_ADMIN
5. WHEN a disabled account's token is used, THE User_Service SHALL reject the request with 401 Unauthorized
6. WHEN a locked account's token is used, THE User_Service SHALL reject the request with 401 Unauthorized
