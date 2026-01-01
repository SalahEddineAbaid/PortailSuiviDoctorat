package com.devbuild.apigateway.filter;

import com.devbuild.apigateway.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * JWT Authentication Filter
 * Validates JWT tokens and adds user information to request headers
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // Headers to add for downstream services
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Processing request: {} {}", request.getMethod(), path);

        // Get route metadata
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route != null) {
            Map<String, Object> metadata = route.getMetadata();

            // Check if route is public (no auth required)
            if (Boolean.TRUE.equals(metadata.get("public"))) {
                log.debug("Public route, skipping authentication: {}", path);
                return chain.filter(exchange);
            }

            // Check if route requires authentication
            if (Boolean.TRUE.equals(metadata.get("requiresAuth"))) {
                String token = extractToken(request);

                if (token == null) {
                    log.warn("No token provided for protected route: {}", path);
                    return onError(exchange, "Missing authorization token", HttpStatus.UNAUTHORIZED);
                }

                try {
                    // Validate token
                    if (!jwtUtil.validateToken(token)) {
                        log.warn("Invalid or expired token for route: {}", path);
                        return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                    }

                    // Check role requirements
                    String requiredRole = (String) metadata.get("requiresRole");
                    if (requiredRole != null && !jwtUtil.hasRole(token, requiredRole)) {
                        log.warn("User does not have required role '{}' for route: {}", requiredRole, path);
                        return onError(exchange, "Insufficient permissions", HttpStatus.FORBIDDEN);
                    }

                    // Extract user information and add to headers
                    exchange = addUserInfoToHeaders(exchange, token);
                    log.debug("Token validated successfully for user: {}", jwtUtil.extractUsername(token));

                } catch (Exception e) {
                    log.error("Error processing JWT token: {}", e.getMessage());
                    return onError(exchange, "Authentication error", HttpStatus.UNAUTHORIZED);
                }
            }
        }

        return chain.filter(exchange);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Add user information to request headers for downstream services
     */
    private ServerWebExchange addUserInfoToHeaders(ServerWebExchange exchange, String token) {
        try {
            Long userId = jwtUtil.extractUserId(token);
            String username = jwtUtil.extractUsername(token);
            List<String> roles = jwtUtil.extractRoles(token);

            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(USER_ID_HEADER, String.valueOf(userId))
                    .header(USERNAME_HEADER, username)
                    .header(ROLES_HEADER, String.join(",", roles))
                    .build();

            return exchange.mutate().request(request).build();
        } catch (Exception e) {
            log.error("Error adding user info to headers: {}", e.getMessage());
            return exchange;
        }
    }

    /**
     * Return error response
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String errorResponse = String.format("{\"error\":\"%s\",\"message\":\"%s\"}",
                status.getReasonPhrase(), message);

        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(errorResponse.getBytes())));
    }

    @Override
    public int getOrder() {
        // Run this filter first (before rate limiting and other filters)
        return -100;
    }
}
