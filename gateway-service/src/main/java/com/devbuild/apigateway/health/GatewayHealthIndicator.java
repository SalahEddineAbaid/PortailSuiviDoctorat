package com.devbuild.apigateway.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Custom Health Indicator for Gateway Service
 */
@Component
@Slf4j
public class GatewayHealthIndicator implements HealthIndicator {

    @Autowired
    private ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Health health() {
        try {
            // Check Redis connectivity
            Boolean redisPing = redisTemplate.getConnectionFactory()
                    .getReactiveConnection()
                    .ping()
                    .map(response -> "PONG".equals(response))
                    .block(Duration.ofSeconds(2));

            if (Boolean.TRUE.equals(redisPing)) {
                return Health.up()
                        .withDetail("redis", "Connected")
                        .withDetail("rateLimit", "Enabled")
                        .build();
            } else {
                return Health.down()
                        .withDetail("redis", "Disconnected")
                        .withDetail("rateLimit", "Disabled")
                        .build();
            }
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
