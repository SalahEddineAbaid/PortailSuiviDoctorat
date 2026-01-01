package com.devbuild.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * JWT Utility class for token validation and claims extraction
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.issuer}")
    private String issuer;

    /**
     * Get signing key from secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Extract all claims from token
     */
    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error extracting claims from token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract user ID from token
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extract roles from token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", List.class));
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract issuer from token
     */
    public String extractIssuer(String token) {
        return extractClaim(token, Claims::getIssuer);
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Validate token
     */
    public Boolean validateToken(String token) {
        try {
            // Check if token is expired
            if (isTokenExpired(token)) {
                log.warn("Token is expired");
                return false;
            }

            // Check issuer
            String tokenIssuer = extractIssuer(token);
            if (!issuer.equals(tokenIssuer)) {
                log.warn("Invalid token issuer. Expected: {}, Got: {}", issuer, tokenIssuer);
                return false;
            }

            log.debug("Token validated successfully for user: {}", extractUsername(token));
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if user has specific role
     */
    public Boolean hasRole(String token, String role) {
        try {
            List<String> roles = extractRoles(token);
            return roles != null && roles.stream()
                    .anyMatch(r -> r.equalsIgnoreCase(role));
        } catch (Exception e) {
            log.error("Error checking role: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if user has any of the specified roles
     */
    public Boolean hasAnyRole(String token, String... roles) {
        try {
            List<String> userRoles = extractRoles(token);
            if (userRoles == null || userRoles.isEmpty()) {
                return false;
            }
            for (String role : roles) {
                if (userRoles.stream().anyMatch(r -> r.equalsIgnoreCase(role))) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking roles: {}", e.getMessage());
            return false;
        }
    }
}
