# Design Document: User Service Finalization

## Overview

This design document specifies the architecture and implementation details for finalizing the user-service microservice. The service currently provides JWT-based authentication, basic user management, and password reset functionality. This finalization adds:

- Detailed user profile management with extended personal information
- Comprehensive audit trail for security monitoring
- Account activation/deactivation with admin controls
- Failed login protection with automatic account locking
- Admin dashboard with statistics and metrics
- Kafka event publishing for inter-service communication

The design follows Spring Boot best practices, uses JPA for persistence, and integrates with existing microservices architecture through Eureka service discovery and Kafka messaging.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      User Service                            │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Controllers  │  │  Services    │  │ Repositories │     │
│  │              │  │              │  │              │     │
│  │ - Auth       │→ │ - Auth       │→ │ - User       │     │
│  │ - User       │  │ - User       │  │ - UserProfile│     │
│  │ - Admin      │  │ - Audit      │  │ - UserAudit  │     │
│  │ - Audit      │  │ - Statistics │  │ - RefreshToken│    │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│         │                  │                                │
│         │                  ├──────────────┐                │
│         │                  │              │                │
│         ▼                  ▼              ▼                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ JWT Security │  │   MariaDB    │  │    Kafka     │    │
│  │   Filter     │  │   Database   │  │  Producer    │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
         │                                      │
         │                                      │
         ▼                                      ▼
┌──────────────────┐                  ┌──────────────────┐
│  Eureka Server   │                  │ Notification     │
│  (Discovery)     │                  │    Service       │
└──────────────────┘                  └──────────────────┘
```

### Component Interaction Flow

**Profile Completion Flow:**

```
Client → PUT /api/users/profile/complete
  → UserController.completeProfile()
    → UserService.completeProfile()
      → UserProfileRepository.save()
      → KafkaProducer.publish(USER_PROFILE_COMPLETED)
    ← UserResponse (with profileComplete=true)
```

**Login with Audit Flow:**

```
Client → POST /api/auth/login
  → AuthController.login()
    → AuthService.login()
      → AuthenticationManager.authenticate()
      → AuditService.logLogin(userId, ipAddress)
      → JwtProvider.generateToken()
      → RefreshTokenRepository.save()
    ← TokenResponse (accessToken, refreshToken)
```

**Account Disable Flow:**

```
Admin → POST /api/admin/users/{id}/disable
  → AdminController.disableUser()
    → UserService.disableUser()
      → User.setAccountStatus(DISABLED)
      → RefreshTokenRepository.deleteByUser()
      → AuditService.logAccountDisabled()
      → KafkaProducer.publish(USER_DISABLED)
    ← Success Response
```

## Components and Interfaces

### 1. Entity Layer

#### UserProfile Entity

```java
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance")
    private String lieuNaissance;

    private String nationalite;

    @Column(unique = true)
    private String cin;

    @Column(name = "photo_url")
    private String photoUrl;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### UserAudit Entity

```java
@Entity
@Table(name = "user_audits", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class UserAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;
}
```

#### Updated User Entity

```java
@Entity
@Table(name = "users")
public class User {
    // ... existing fields ...

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "lockout_expiration")
    private LocalDateTime lockoutExpiration;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;
}
```

#### Enums

```java
public enum AccountStatus {
    ACTIVE,
    DISABLED,
    LOCKED
}

public enum AuditAction {
    LOGIN,
    LOGIN_FAILED,
    LOGOUT,
    PASSWORD_CHANGE,
    ROLE_ASSIGNED,
    ROLE_REMOVED,
    PROFILE_MODIFIED,
    ACCOUNT_DISABLED,
    ACCOUNT_ENABLED,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED
}
```

### 2. DTO Layer

#### Request DTOs

```java
public record ProfileCompleteRequest(
    @NotNull LocalDate dateNaissance,
    @NotBlank String lieuNaissance,
    @NotBlank String nationalite,
    @NotBlank @Pattern(regexp = "^[A-Z]{1,2}\\d{5,6}$") String cin,
    String photoUrl
) {}

public record DisableAccountRequest(
    @NotBlank String reason
) {}
```

#### Response DTOs

```java
public record UserDetailedResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String phoneNumber,
    String adresse,
    String ville,
    String pays,
    Set<String> roles,
    boolean profileComplete,
    ProfileData profile,
    AccountStatus accountStatus
) {}

public record ProfileData(
    LocalDate dateNaissance,
    String lieuNaissance,
    String nationalite,
    String cin,
    String photoUrl
) {}

public record AuditRecordResponse(
    Long id,
    Long userId,
    AuditAction action,
    LocalDateTime timestamp,
    String ipAddress,
    String details
) {}

public record UserStatisticsResponse(
    long total,
    Map<String, Long> byRole,
    long active,
    long disabled,
    long locked,
    long newThisMonth
) {}

public record ConnectionStatisticsResponse(
    List<DailyConnectionCount> dailyCounts,
    Map<String, Long> byRole
) {}

public record DailyConnectionCount(
    LocalDate date,
    long count
) {}
```

### 3. Repository Layer

```java
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserId(Long userId);
    boolean existsByCin(String cin);
}

public interface UserAuditRepository extends JpaRepository<UserAudit, Long> {
    Page<UserAudit> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    Page<UserAudit> findByUserIdAndActionOrderByTimestampDesc(
        Long userId, AuditAction action, Pageable pageable);
    List<UserAudit> findByTimestampAfter(LocalDateTime timestamp);
    long countByActionAndTimestampBetween(
        AuditAction action, LocalDateTime start, LocalDateTime end);
    List<UserAudit> findByActionAndTimestampBetween(
        AuditAction action, LocalDateTime start, LocalDateTime end);
}

// Extended UserRepository
public interface UserRepository extends JpaRepository<User, Long> {
    // ... existing methods ...
    long countByAccountStatus(AccountStatus status);
    Page<User> findByAccountStatus(AccountStatus status, Pageable pageable);
    long countByCreatedAtAfter(LocalDateTime date);
}
```

### 4. Service Layer

#### UserService (Extended)

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final AuditService auditService;
    private final UserEventPublisher eventPublisher;

    public UserDetailedResponse completeProfile(Long userId, ProfileCompleteRequest request) {
        // Validate user exists
        // Check CIN uniqueness
        // Create or update UserProfile
        // Set profileComplete flag
        // Publish event
        // Return detailed response
    }

    public UserDetailedResponse getDetailedProfile(Long userId) {
        // Fetch user with profile
        // Check profileComplete status
        // Return detailed response
    }

    public void disableAccount(Long userId, String reason, Long adminId) {
        // Validate admin cannot disable self
        // Set account status to DISABLED
        // Invalidate all refresh tokens
        // Log audit event
        // Publish USER_DISABLED event
    }

    public void enableAccount(Long userId, Long adminId) {
        // Set account status to ACTIVE
        // Reset failed login attempts
        // Log audit event
        // Publish USER_ENABLED event
    }

    public Page<UserResponse> getDisabledAccounts(Pageable pageable) {
        // Query disabled accounts
        // Map to response DTOs
    }
}
```

#### AuditService

```java
@Service
public class AuditService {
    private final UserAuditRepository auditRepository;

    public void logLogin(Long userId, String ipAddress, boolean success) {
        // Create audit record with LOGIN or LOGIN_FAILED
    }

    public void logPasswordChange(Long userId, String ipAddress) {
        // Create audit record with PASSWORD_CHANGE
    }

    public void logRoleAssigned(Long userId, String role, Long adminId) {
        // Create audit record with ROLE_ASSIGNED
    }

    public void logProfileModified(Long userId, String ipAddress) {
        // Create audit record with PROFILE_MODIFIED
    }

    public void logAccountDisabled(Long userId, String reason, Long adminId) {
        // Create audit record with ACCOUNT_DISABLED
    }

    public void logAccountEnabled(Long userId, Long adminId) {
        // Create audit record with ACCOUNT_ENABLED
    }

    public Page<AuditRecordResponse> getUserAuditHistory(
        Long userId, AuditAction action, Pageable pageable) {
        // Query audit records with optional action filter
    }

    public List<AuditRecordResponse> getRecentAudits() {
        // Get audits from last 24 hours
    }
}
```

#### StatisticsService

```java
@Service
public class StatisticsService {
    private final UserRepository userRepository;
    private final UserAuditRepository auditRepository;

    public UserStatisticsResponse getUserStatistics() {
        // Count total users
        // Count by role
        // Count by account status
        // Count new users this month
    }

    public ConnectionStatisticsResponse getConnectionStatistics() {
        // Query LOGIN audits for last 30 days
        // Group by date
        // Group by role
    }
}
```

#### LoginAttemptService

```java
@Service
public class LoginAttemptService {
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${user-service.account.max-failed-logins:5}")
    private int maxFailedLogins;

    @Value("${user-service.account.lockout-duration:30m}")
    private Duration lockoutDuration;

    public void loginSucceeded(User user) {
        // Reset failed login attempts
        // Clear lockout expiration
    }

    public void loginFailed(User user, String ipAddress) {
        // Increment failed login attempts
        // Log failed login audit
        // If max attempts reached, lock account
    }

    public boolean isAccountLocked(User user) {
        // Check if lockout expiration is in future
        // Auto-unlock if expiration passed
    }
}
```

#### UserEventPublisher

```java
@Service
public class UserEventPublisher {
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private static final String TOPIC = "user-events";

    public void publishUserRegistered(User user) {
        // Publish USER_REGISTERED event
    }

    public void publishUserDisabled(Long userId, String reason) {
        // Publish USER_DISABLED event
    }

    public void publishUserEnabled(Long userId) {
        // Publish USER_ENABLED event
    }

    public void publishPasswordChanged(Long userId) {
        // Publish PASSWORD_CHANGED event
    }

    public void publishRoleAssigned(Long userId, String role) {
        // Publish ROLE_ASSIGNED event
    }
}
```

### 5. Controller Layer

#### UserController (Extended)

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PutMapping("/profile/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDetailedResponse> completeProfile(
        @Valid @RequestBody ProfileCompleteRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        // Get current user ID from authentication
        // Call userService.completeProfile()
        // Return 200 OK with detailed response
    }

    @GetMapping("/{id}/profile-complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDetailedResponse> getDetailedProfile(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails) {
        // Check if user is accessing own profile or is admin
        // Call userService.getDetailedProfile()
        // Return 200 OK with detailed response
    }
}
```

#### AdminController

```java
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @PostMapping("/users/{userId}/disable")
    public ResponseEntity<Void> disableUser(
        @PathVariable Long userId,
        @Valid @RequestBody DisableAccountRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {
        // Get admin user ID
        // Call userService.disableAccount()
        // Return 204 No Content
    }

    @PostMapping("/users/{userId}/enable")
    public ResponseEntity<Void> enableUser(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails userDetails) {
        // Get admin user ID
        // Call userService.enableAccount()
        // Return 204 No Content
    }

    @GetMapping("/users/disabled")
    public ResponseEntity<Page<UserResponse>> getDisabledUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        // Call userService.getDisabledAccounts()
        // Return 200 OK with paginated results
    }

    @GetMapping("/statistics/users")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {
        // Call statisticsService.getUserStatistics()
        // Return 200 OK with statistics
    }

    @GetMapping("/statistics/connections")
    public ResponseEntity<ConnectionStatisticsResponse> getConnectionStatistics() {
        // Call statisticsService.getConnectionStatistics()
        // Return 200 OK with connection stats
    }
}
```

#### AuditController

```java
@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<AuditRecordResponse>> getUserAuditHistory(
        @PathVariable Long userId,
        @RequestParam(required = false) AuditAction action,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        // Call auditService.getUserAuditHistory()
        // Return 200 OK with paginated audit records
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AuditRecordResponse>> getRecentAudits() {
        // Call auditService.getRecentAudits()
        // Return 200 OK with recent audits
    }
}
```

### 6. Security Layer Updates

#### Updated AuthService

```java
@Service
public class AuthService {
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;

    public TokenResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (loginAttemptService.isAccountLocked(user)) {
            auditService.logLogin(user.getId(), ipAddress, false);
            throw new AccountLockedException("Account is temporarily locked");
        }

        // Check if account is disabled
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            auditService.logLogin(user.getId(), ipAddress, false);
            throw new DisabledException("Account is disabled");
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.email(), request.password()));

            // Login succeeded
            loginAttemptService.loginSucceeded(user);
            auditService.logLogin(user.getId(), ipAddress, true);

            // Generate tokens
            String accessToken = jwtProvider.generateToken(authentication);
            String refreshToken = generateRefreshToken(user);

            return new TokenResponse(accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            // Login failed
            loginAttemptService.loginFailed(user, ipAddress);
            throw e;
        }
    }
}
```

#### IP Address Extraction

```java
@Component
public class IpAddressExtractor {
    public String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

## Data Models

### Database Schema

```sql
-- User Profiles Table
CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    date_naissance DATE,
    lieu_naissance VARCHAR(255),
    nationalite VARCHAR(100),
    cin VARCHAR(20) UNIQUE,
    photo_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- User Audits Table
CREATE TABLE user_audits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    details TEXT,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_timestamp (timestamp)
);

-- Update Users Table
ALTER TABLE users
ADD COLUMN account_status VARCHAR(20) DEFAULT 'ACTIVE',
ADD COLUMN failed_login_attempts INT DEFAULT 0,
ADD COLUMN lockout_expiration TIMESTAMP NULL;
```

### Kafka Event Schema

```java
public record UserEvent(
    String eventType,  // USER_REGISTERED, USER_DISABLED, etc.
    Long userId,
    String email,
    LocalDateTime timestamp,
    Map<String, Object> metadata
) {}
```

## Correctness Properties

_A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees._

### Property 1: Profile Completion Idempotence

_For any_ user with a complete profile, submitting profile completion data multiple times should result in the profile being updated with the latest data, and profileComplete should remain true.
**Validates: Requirements 1.1, 1.4**

### Property 2: CIN Uniqueness

_For any_ two different users, their CIN values must be unique across the system, and attempting to use a duplicate CIN should be rejected.
**Validates: Requirements 1.6**

### Property 3: Audit Trail Completeness

_For any_ security-sensitive action (login, password change, role assignment, profile modification), an audit record must be created with all required fields populated.
**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

### Property 4: Account Disable Prevents Login

_For any_ user with account status DISABLED, all login attempts should be rejected regardless of correct credentials.
**Validates: Requirements 3.3**

### Property 5: Self-Disable Prevention

_For any_ admin user, attempting to disable their own account should be rejected with an appropriate error.
**Validates: Requirements 3.6**

### Property 6: Token Invalidation on Disable

_For any_ user account that is disabled, all existing refresh tokens for that user should be deleted from the database.
**Validates: Requirements 3.8**

### Property 7: Failed Login Counter Reset

_For any_ user who successfully logs in, the failed login attempts counter should be reset to zero.
**Validates: Requirements 4.6**

### Property 8: Account Locking After Max Attempts

_For any_ user who fails login attempts equal to the configured maximum, the account should be locked with a lockout expiration time set.
**Validates: Requirements 4.2, 4.3**

### Property 9: Automatic Unlock After Expiration

_For any_ locked account where the lockout expiration time has passed, the account should be automatically unlocked when checked.
**Validates: Requirements 4.5**

### Property 10: Statistics Accuracy

_For any_ point in time, user statistics should accurately reflect the current database state including counts by role, status, and creation date.
**Validates: Requirements 5.1, 5.5**

### Property 11: Event Publishing Non-Blocking

_For any_ user operation (register, disable, enable, password change, role assign), if event publishing fails, the primary operation should still succeed.
**Validates: Requirements 6.7**

### Property 12: Admin Authorization Enforcement

_For any_ admin endpoint, requests from non-admin users should be rejected with 403 Forbidden status.
**Validates: Requirements 7.10, 10.4**

### Property 13: Profile Access Authorization

_For any_ user attempting to access another user's detailed profile, the request should only succeed if the requester is an admin.
**Validates: Requirements 10.3**

## Error Handling

### Exception Hierarchy

```java
public class AccountLockedException extends AuthenticationException {}
public class AccountDisabledException extends AuthenticationException {}
public class SelfDisableException extends BusinessException {}
public class DuplicateCinException extends BusinessException {}
public class ProfileIncompleteException extends BusinessException {}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("ACCOUNT_LOCKED", ex.getMessage()));
    }

    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(AccountDisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("ACCOUNT_DISABLED", ex.getMessage()));
    }

    @ExceptionHandler(SelfDisableException.class)
    public ResponseEntity<ErrorResponse> handleSelfDisable(SelfDisableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("SELF_DISABLE_NOT_ALLOWED", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateCinException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCin(DuplicateCinException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("DUPLICATE_CIN", ex.getMessage()));
    }
}
```

### Error Response Format

```java
public record ErrorResponse(
    String code,
    String message,
    LocalDateTime timestamp,
    Map<String, String> details
) {
    public ErrorResponse(String code, String message) {
        this(code, message, LocalDateTime.now(), Map.of());
    }
}
```

## Testing Strategy

### Unit Testing

Unit tests verify specific examples, edge cases, and error conditions for individual components. Focus on:

- **Service Layer Logic**: Test business rules, validation, and error handling
- **Security Components**: Test JWT generation, validation, and authentication flows
- **Utility Classes**: Test IP extraction, date calculations, and data transformations

**Key Unit Test Examples:**

- `testCompleteProfile_WithValidData_CreatesProfile`
- `testCompleteProfile_WithDuplicateCin_ThrowsException`
- `testDisableAccount_SelfDisable_ThrowsException`
- `testLoginFailed_MaxAttempts_LocksAccount`
- `testIsAccountLocked_ExpiredLockout_ReturnsUnlocked`

### Property-Based Testing

Property tests verify universal properties across all inputs using randomized test data. Each test runs minimum 100 iterations.

**Framework**: Use JUnit 5 with jqwik for property-based testing in Java.

**Property Test Configuration:**

```java
@Property(tries = 100)
@Tag("Feature: user-service-finalization, Property 1: Profile Completion Idempotence")
void profileCompletionIsIdempotent(@ForAll User user, @ForAll ProfileCompleteRequest request) {
    // Test implementation
}
```

**Key Property Tests:**

- Profile completion idempotence (Property 1)
- CIN uniqueness enforcement (Property 2)
- Audit trail completeness (Property 3)
- Account disable prevents login (Property 4)
- Failed login counter reset (Property 7)
- Account locking after max attempts (Property 8)
- Statistics accuracy (Property 10)

### Integration Testing

Integration tests verify end-to-end flows using TestContainers for MariaDB and Kafka:

- Complete authentication flow with audit logging
- Profile completion with event publishing
- Account disable flow with token invalidation
- Failed login protection with account locking
- Admin statistics retrieval

**TestContainers Setup:**

```java
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {
    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.6");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
}
```

### Test Coverage Target

- Overall coverage: 80%
- Critical security components: 90%
- Service layer: 85%
- Controller layer: 75%

## Configuration

### application.yml Updates

```yaml
user-service:
  account:
    max-failed-logins: 5
    lockout-duration: 30m
  audit:
    enabled: true
    retention-days: 365

spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    topic:
      user-events: user-events
```

### Kafka Configuration

```java
@Configuration
public class KafkaProducerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, UserEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, UserEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### Maven Dependencies to Add

```xml
<!-- Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Property-Based Testing -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.2</version>
    <scope>test</scope>
</dependency>

<!-- TestContainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mariadb</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>
```
