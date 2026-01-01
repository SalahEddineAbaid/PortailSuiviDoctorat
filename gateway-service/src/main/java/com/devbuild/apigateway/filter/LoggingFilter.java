package com.devbuild.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Logging Filter for request/response tracking
 */
@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String START_TIME_ATTRIBUTE = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // Generate or get existing request ID
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }

        // Add request ID to headers for downstream services
        final String finalRequestId = requestId;
        ServerHttpRequest modifiedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, finalRequestId)
                .build();

        // Record start time
        exchange.getAttributes().put(START_TIME_ATTRIBUTE, System.currentTimeMillis());

        // Log request
        log.info("Incoming request: {} {} | RequestId: {} | IP: {}",
                request.getMethod(),
                request.getURI().getPath(),
                finalRequestId,
                request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress()
                        : "unknown");

        return chain.filter(exchange.mutate().request(modifiedRequest).build())
                .doFinally(signalType -> {
                    // Log response
                    Long startTime = exchange.getAttribute(START_TIME_ATTRIBUTE);
                    long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

                    log.info("Outgoing response: {} {} | RequestId: {} | Status: {} | Duration: {}ms",
                            request.getMethod(),
                            request.getURI().getPath(),
                            finalRequestId,
                            exchange.getResponse().getStatusCode(),
                            duration);
                });
    }

    @Override
    public int getOrder() {
        // Run early to log all requests
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
