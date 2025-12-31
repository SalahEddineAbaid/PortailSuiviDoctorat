package ma.emsi.notificationservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtProvider.
 * Tests JWT token validation and claim extraction.
 */
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private String jwtSecret = "mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890";

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", jwtSecret);
    }

    @Test
    void testValidateToken_ValidToken_ReturnsTrue() {
        // Given: a valid JWT token
        String token = createTestToken("test@example.com", Arrays.asList("ROLE_ADMIN"));

        // When: validating the token
        boolean isValid = jwtProvider.validateToken(token);

        // Then: token should be valid
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsFalse() {
        // Given: an invalid JWT token
        String invalidToken = "invalid.jwt.token";

        // When: validating the token
        boolean isValid = jwtProvider.validateToken(invalidToken);

        // Then: token should be invalid
        assertFalse(isValid);
    }

    @Test
    void testValidateToken_ExpiredToken_ReturnsFalse() {
        // Given: an expired JWT token
        String expiredToken = createExpiredToken("test@example.com");

        // When: validating the token
        boolean isValid = jwtProvider.validateToken(expiredToken);

        // Then: token should be invalid
        assertFalse(isValid);
    }

    @Test
    void testGetEmailFromToken_ValidToken_ReturnsEmail() {
        // Given: a valid JWT token with email
        String email = "test@example.com";
        String token = createTestToken(email, Arrays.asList("ROLE_USER"));

        // When: extracting email from token
        String extractedEmail = jwtProvider.getEmailFromToken(token);

        // Then: email should match
        assertEquals(email, extractedEmail);
    }

    @Test
    void testGetRolesFromToken_ValidToken_ReturnsRoles() {
        // Given: a valid JWT token with roles
        String token = createTestToken("test@example.com", Arrays.asList("ROLE_ADMIN", "ROLE_USER"));

        // When: extracting roles from token
        var roles = jwtProvider.getRolesFromToken(token);

        // Then: roles should match
        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    void testIsTokenExpired_ValidToken_ReturnsFalse() {
        // Given: a valid non-expired token
        String token = createTestToken("test@example.com", Arrays.asList("ROLE_USER"));

        // When: checking if token is expired
        boolean isExpired = jwtProvider.isTokenExpired(token);

        // Then: token should not be expired
        assertFalse(isExpired);
    }

    @Test
    void testIsTokenExpired_ExpiredToken_ReturnsTrue() {
        // Given: an expired token
        String expiredToken = createExpiredToken("test@example.com");

        // When: checking if token is expired
        boolean isExpired = jwtProvider.isTokenExpired(expiredToken);

        // Then: token should be expired
        assertTrue(isExpired);
    }

    // Helper methods

    private String createTestToken(String email, java.util.List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(key)
                .compact();
    }

    private String createExpiredToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", Arrays.asList("ROLE_USER"));

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(key)
                .compact();
    }
}
