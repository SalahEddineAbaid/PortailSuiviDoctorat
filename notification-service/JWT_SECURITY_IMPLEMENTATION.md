# JWT Security Implementation

## Overview

This document describes the JWT security implementation for the notification-service. The service validates JWT tokens issued by the user-service and enforces role-based access control for all API endpoints.

## Architecture

### Components

1. **JwtProvider** - Validates JWT tokens and extracts claims (email, roles)
2. **JwtAuthenticationFilter** - Intercepts requests, validates JWT, and populates SecurityContext
3. **JwtAuthenticationEntryPoint** - Handles authentication errors (returns 401)
4. **SecurityConfig** - Configures Spring Security with JWT authentication and CORS

### Flow

```
Client Request → JwtAuthenticationFilter → JwtProvider (validate) → SecurityContext → Controller (@PreAuthorize)
```

## Requirements Validation

### Requirement 12.1: JWT Token Required
- All `/api/notifications/**` endpoints require a valid JWT token in the Authorization header
- Implemented in `SecurityConfig.securityFilterChain()`
- Format: `Authorization: Bearer <token>`

### Requirement 12.2: Admin Role Verification
- Admin-only endpoints use `@PreAuthorize("hasRole('ADMIN')")`
- Examples:
  - `GET /api/notifications` - List all notifications
  - `GET /api/notifications/stats` - Get statistics
  - `POST /api/notifications/{id}/retry` - Retry failed notification
  - `POST /api/notifications/dlq/retry-all` - Retry all DLQ messages

### Requirement 12.3: Email Matching Verification
- User-specific endpoints verify email matches JWT principal
- Example: `GET /api/notifications/user/{email}` uses `@PreAuthorize("hasRole('ADMIN') or #email == authentication.principal")`
- Users can only access their own notifications unless they have ADMIN role

### Requirement 12.4: Invalid JWT Rejection
- Invalid or expired tokens return HTTP 401 Unauthorized
- Implemented in `JwtAuthenticationEntryPoint.commence()`
- Returns JSON response with error details

### Requirement 12.5: Insufficient Role Rejection
- Users without required roles receive HTTP 403 Forbidden
- Handled by Spring Security's access denied handler
- Triggered when `@PreAuthorize` conditions fail

### Requirement 12.6: SecurityContext Population
- `JwtAuthenticationFilter` extracts email and roles from JWT
- Creates `UsernamePasswordAuthenticationToken` with authorities
- Populates `SecurityContextHolder` for downstream use

## Configuration

### Application Properties

```properties
# JWT Configuration
jwt.secret=${JWT_SECRET:mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890}
jwt.expiration=86400000
```

**Important**: In production, set `JWT_SECRET` as an environment variable. The secret must match the one used by user-service.

### CORS Configuration

CORS is configured to allow requests from the Angular frontend:
- Allowed Origin: `http://localhost:4200`
- Allowed Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH
- Allowed Headers: All
- Credentials: Enabled
- Max Age: 3600 seconds

## API Endpoints Security

### Public Endpoints
- `/actuator/**` - Health checks and metrics
- `/error` - Error handling

### Protected Endpoints (Authenticated)
All `/api/notifications/**` endpoints require authentication.

#### Admin Only
- `GET /api/notifications` - List all notifications (paginated)
- `GET /api/notifications/status/{status}` - Filter by status
- `GET /api/notifications/stats` - Get statistics
- `GET /api/notifications/failed` - List failed notifications
- `GET /api/notifications/search` - Search with filters
- `POST /api/notifications/{id}/retry` - Retry failed notification
- `POST /api/notifications/dlq/retry-all` - Retry all DLQ messages

#### Admin or Owner
- `GET /api/notifications/{id}` - Get specific notification (admin or owner)
- `GET /api/notifications/user/{email}` - Get user's notifications (admin or matching email)

## Testing

### Unit Tests

**JwtProviderTest** - Tests JWT validation and claim extraction:
- Valid token validation
- Invalid token rejection
- Expired token detection
- Email extraction
- Role extraction

**SecurityConfigTest** - Integration tests for security rules:
- Public endpoints accessible without auth
- Protected endpoints require JWT
- Invalid tokens return 401

### Manual Testing with Postman

1. **Get JWT Token from user-service**:
   ```
   POST http://localhost:8081/api/auth/login
   Body: { "email": "admin@example.com", "password": "password" }
   ```

2. **Use Token in notification-service**:
   ```
   GET http://localhost:8084/api/notifications
   Headers: Authorization: Bearer <token>
   ```

3. **Test Authorization**:
   - Admin token should access all endpoints
   - User token should only access own notifications
   - No token should return 401
   - Wrong role should return 403

## Security Best Practices

1. **Token Validation**: Every request validates the JWT signature and expiration
2. **Stateless Sessions**: No server-side session storage (SessionCreationPolicy.STATELESS)
3. **CORS Protection**: Only allows requests from configured origins
4. **Role-Based Access**: Fine-grained control with @PreAuthorize annotations
5. **Error Handling**: Proper HTTP status codes (401, 403) for security errors

## Troubleshooting

### 401 Unauthorized
- Check if JWT token is included in Authorization header
- Verify token format: `Bearer <token>`
- Ensure token is not expired
- Verify JWT secret matches user-service

### 403 Forbidden
- Check user roles in JWT token
- Verify @PreAuthorize annotation on endpoint
- Ensure role names match (e.g., "ROLE_ADMIN" vs "ADMIN")

### CORS Errors
- Verify frontend origin is in allowed origins list
- Check if preflight OPTIONS requests are succeeding
- Ensure credentials are enabled if using cookies

## Integration with Other Services

### user-service
- Issues JWT tokens during login
- Includes email and roles in token claims
- Uses same JWT secret for signing

### inscription-service
- May call notification-service endpoints (future)
- Would need service-to-service authentication
- Consider using API keys or service accounts

## Future Enhancements

1. **Refresh Tokens**: Implement token refresh mechanism
2. **Service-to-Service Auth**: Add API keys for microservice communication
3. **Rate Limiting**: Add rate limiting per user/IP
4. **Audit Logging**: Log all authentication attempts
5. **Token Revocation**: Implement token blacklist for logout
