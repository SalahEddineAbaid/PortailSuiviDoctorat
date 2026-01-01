package com.devbuild.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * Redis-based Rate Limiting Filter
 * Implements sliding window rate limiting per IP or per user
 */
@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate-limit.default-limit:100}")
    private int defaultLimit;

    @Value("${rate-limit.unauthenticated-limit:50}")
    private int unauthenticatedLimit;

    @Value("${rate-limit.admin-limit:500}")
    private int adminLimit;

    @Value("${rate-limit.window-size:60}")
    private int windowSizeSeconds;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!rateLimitEnabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String clientId = getClientIdentifier(request);
        int limit = getRateLimit(exchange, request);

        String key = RATE_LIMIT_KEY_PREFIX + clientId;
        long now = Instant.now().getEpochSecond();
        long windowStart = now - windowSizeSeconds;

        return redisTemplate.opsForZSet()
                // Remove old entries outside the window
                .removeRangeByScore(key, org.springframework.data.domain.Range.closed(0.0, (double) windowStart))
                .then(redisTemplate.opsForZSet().count(key,
                        org.springframework.data.domain.Range.closed((double) windowStart, (double) now)))
                .flatMap(count -> {
                    if (count >= limit) {
                        log.warn("Rate limit exceeded for client: {} (limit: {}, current: {})",
                                clientId, limit, count);
                        return onRateLimitExceeded(exchange, limit);
                    }

                    // Add current request to the window
                    return redisTemplate.opsForZSet()
                            .add(key, String.valueOf(now), now)
                            .then(redisTemplate.expire(key, Duration.ofSeconds(windowSizeSeconds)))
                            .then(Mono.defer(() -> {
                                // Add rate limit headers
                                ServerHttpResponse response = exchange.getResponse();
                                response.getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
                                response.getHeaders().add("X-RateLimit-Remaining",
                                        String.valueOf(Math.max(0, limit - count - 1)));
                                response.getHeaders().add("X-RateLimit-Reset",
                                        String.valueOf(now + windowSizeSeconds));

                                log.debug("Rate limit check passed for client: {} ({}/{})",
                                        clientId, count + 1, limit);
                                return chain.filter(exchange);
                            }));
                });
    }

    /**
     * Get client identifier (user ID if authenticated, IP address otherwise)
     */
    private String getClientIdentifier(ServerHttpRequest request) {
        String userId = request.getHeaders().getFirst(USER_ID_HEADER);
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }

        // Use IP address for unauthenticated requests
        String ip = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ip;
    }

    /**
     * Get rate limit based on route metadata and user roles
     */
    private int getRateLimit(ServerWebExchange exchange, ServerHttpRequest request) {
        // Check if user is admin
        String roles = request.getHeaders().getFirst(ROLES_HEADER);
        if (roles != null && roles.contains("ADMIN")) {
            return adminLimit;
        }

        // Check route-specific limit
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route != null) {
            Map<String, Object> metadata = route.getMetadata();
            Object routeLimit = metadata.get("rateLimit");
            if (routeLimit instanceof Integer) {
                return (Integer) routeLimit;
            }
        }

        // Use default limit for authenticated users, lower limit for unauthenticated
        String userId = request.getHeaders().getFirst(USER_ID_HEADER);
        return userId != null ? defaultLimit : unauthenticatedLimit;
    }

    /**
     * Return rate limit exceeded error
     */
    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange, int limit) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add("Content-Type", "application/json");
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("Retry-After", String.valueOf(windowSizeSeconds));

        String errorResponse = String.format(
                "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Maximum %d requests per %d seconds.\"}",
                limit, windowSizeSeconds);

        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(errorResponse.getBytes())));
    }

    @Override
    public int getOrder() {
        // Run after authentication filter
        return -50;
    }
}
