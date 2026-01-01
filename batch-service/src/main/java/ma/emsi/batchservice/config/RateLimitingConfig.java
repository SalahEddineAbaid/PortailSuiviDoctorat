package ma.emsi.batchservice.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting Configuration
 * Configures Resilience4j RateLimiter for job trigger endpoints
 * Requirement: 10.10
 */
@Configuration
@Slf4j
public class RateLimitingConfig {

    /**
     * Configure rate limiter for job trigger endpoints
     * Limits: 5 requests per 60 seconds per user
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(5) // 5 requests
                .limitRefreshPeriod(Duration.ofSeconds(60)) // per 60 seconds
                .timeoutDuration(Duration.ofSeconds(0)) // fail immediately if limit exceeded
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);

        // Create named rate limiter for job trigger endpoints
        RateLimiter jobTriggerLimiter = registry.rateLimiter("jobTrigger", config);

        // Add event listeners for monitoring
        jobTriggerLimiter.getEventPublisher()
                .onSuccess(event -> log.debug("Rate limiter: Request allowed"))
                .onFailure(event -> log.warn("Rate limiter: Request rejected - limit exceeded"));

        log.info("Rate limiter configured: 5 requests per 60 seconds for job trigger endpoints");

        return registry;
    }

    /**
     * Configure custom rate limiter for specific scenarios
     * This can be used for different rate limits on different endpoints
     */
    @Bean
    public RateLimiter customRateLimiter(RateLimiterRegistry registry) {
        // Create a more restrictive rate limiter for sensitive operations
        RateLimiterConfig strictConfig = RateLimiterConfig.custom()
                .limitForPeriod(2) // 2 requests
                .limitRefreshPeriod(Duration.ofMinutes(5)) // per 5 minutes
                .timeoutDuration(Duration.ofSeconds(0))
                .build();

        RateLimiter strictLimiter = registry.rateLimiter("strictLimit", strictConfig);

        strictLimiter.getEventPublisher()
                .onSuccess(event -> log.debug("Strict rate limiter: Request allowed"))
                .onFailure(event -> log.warn("Strict rate limiter: Request rejected - limit exceeded"));

        return strictLimiter;
    }
}
