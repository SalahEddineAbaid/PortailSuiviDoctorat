package ma.emsi.notificationservice.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to verify Resilience4j configuration properties are correctly loaded
 * and match the requirements specified in the design document.
 * 
 * Requirements tested:
 * - 7.1, 7.2, 7.3: Retry with exponential backoff
 * - 8.1, 8.2, 8.3, 8.4, 8.5, 8.6: Circuit breaker configuration
 * - 9.1, 9.2: Time limiter configuration
 * - 10.1, 10.2: Bulkhead configuration
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
class ResilienceConfigurationTest {

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private RetryRegistry retryRegistry;

    @Autowired(required = false)
    private TimeLimiterRegistry timeLimiterRegistry;

    @Autowired(required = false)
    private BulkheadRegistry bulkheadRegistry;

    /**
     * Test circuit breaker configuration matches requirements 8.1-8.6
     * - 50% failure rate threshold
     * - Sliding window of 10 calls
     * - 60 seconds wait in open state
     * - 3 permitted calls in half-open state
     */
    @Test
    void testCircuitBreakerConfiguration() {
        if (circuitBreakerRegistry != null) {
            var circuitBreaker = circuitBreakerRegistry.circuitBreaker("emailService");
            var config = circuitBreaker.getCircuitBreakerConfig();

            // Requirement 8.1: 50% failure rate threshold
            assertThat(config.getFailureRateThreshold())
                .as("Circuit breaker should open at 50% failure rate")
                .isEqualTo(50.0f);

            // Requirement 8.1: Sliding window of 10 calls
            assertThat(config.getSlidingWindowSize())
                .as("Circuit breaker should use sliding window of 10 calls")
                .isEqualTo(10);

            // Requirement 8.2: 60 seconds wait in open state
            assertThat(config.getWaitIntervalFunctionInOpenState().apply(1))
                .as("Circuit breaker should wait 60 seconds in open state")
                .isEqualTo(60000L);

            // Requirement 8.4: 3 permitted calls in half-open state
            assertThat(config.getPermittedNumberOfCallsInHalfOpenState())
                .as("Circuit breaker should permit 3 test calls in half-open state")
                .isEqualTo(3);

            // Verify minimum number of calls before calculating failure rate
            assertThat(config.getMinimumNumberOfCalls())
                .as("Circuit breaker should require minimum 5 calls")
                .isEqualTo(5);
        }
    }

    /**
     * Test retry configuration matches requirements 7.1-7.3
     * - 3 max attempts
     * - 5 seconds initial wait
     * - Exponential backoff with multiplier 2 (5s, 10s, 20s)
     */
    @Test
    void testRetryConfiguration() {
        if (retryRegistry != null) {
            var retry = retryRegistry.retry("emailService");
            var config = retry.getRetryConfig();

            // Requirement 7.1: 3 max attempts (including initial attempt)
            assertThat(config.getMaxAttempts())
                .as("Retry should attempt maximum 3 times")
                .isEqualTo(3);

            // Requirement 7.2: 5 seconds initial wait
            assertThat(config.getIntervalBiFunction().apply(1, null))
                .as("First retry should wait 5 seconds")
                .isEqualTo(5000L);

            // Requirement 7.3: Exponential backoff with multiplier 2
            // Second retry: 5s * 2 = 10s
            assertThat(config.getIntervalBiFunction().apply(2, null))
                .as("Second retry should wait 10 seconds (5s * 2)")
                .isEqualTo(10000L);

            // Third retry: 10s * 2 = 20s
            assertThat(config.getIntervalBiFunction().apply(3, null))
                .as("Third retry should wait 20 seconds (10s * 2)")
                .isEqualTo(20000L);
        }
    }

    /**
     * Test time limiter configuration matches requirement 9.1
     * - 30 seconds timeout
     */
    @Test
    void testTimeLimiterConfiguration() {
        if (timeLimiterRegistry != null) {
            var timeLimiter = timeLimiterRegistry.timeLimiter("emailService");
            var config = timeLimiter.getTimeLimiterConfig();

            // Requirement 9.1: 30 seconds timeout
            assertThat(config.getTimeoutDuration())
                .as("Time limiter should have 30 second timeout")
                .isEqualTo(Duration.ofSeconds(30));

            // Verify that running future is cancelled on timeout
            assertThat(config.shouldCancelRunningFuture())
                .as("Time limiter should cancel running future on timeout")
                .isTrue();
        }
    }

    /**
     * Test bulkhead configuration matches requirements 10.1-10.2
     * - 10 max concurrent calls
     * - 5 seconds max wait duration
     */
    @Test
    void testBulkheadConfiguration() {
        if (bulkheadRegistry != null) {
            var bulkhead = bulkheadRegistry.bulkhead("emailService");
            var config = bulkhead.getBulkheadConfig();

            // Requirement 10.1: 10 max concurrent calls
            assertThat(config.getMaxConcurrentCalls())
                .as("Bulkhead should limit to 10 concurrent calls")
                .isEqualTo(10);

            // Requirement 10.2: 5 seconds max wait duration
            assertThat(config.getMaxWaitDuration())
                .as("Bulkhead should wait maximum 5 seconds")
                .isEqualTo(Duration.ofSeconds(5));
        }
    }

    /**
     * Verify that all resilience4j registries are properly configured
     */
    @Test
    void testAllResilienceRegistriesAreConfigured() {
        assertThat(circuitBreakerRegistry)
            .as("CircuitBreakerRegistry should be configured")
            .isNotNull();

        assertThat(retryRegistry)
            .as("RetryRegistry should be configured")
            .isNotNull();

        assertThat(timeLimiterRegistry)
            .as("TimeLimiterRegistry should be configured")
            .isNotNull();

        assertThat(bulkheadRegistry)
            .as("BulkheadRegistry should be configured")
            .isNotNull();
    }

    /**
     * Verify emailService instance exists in all registries
     */
    @Test
    void testEmailServiceInstancesExist() {
        if (circuitBreakerRegistry != null) {
            assertThat(circuitBreakerRegistry.getAllCircuitBreakers())
                .as("CircuitBreaker registry should contain emailService")
                .extracting(CircuitBreaker::getName)
                .contains("emailService");
        }

        if (retryRegistry != null) {
            assertThat(retryRegistry.getAllRetries())
                .as("Retry registry should contain emailService")
                .extracting(retry -> retry.getName())
                .contains("emailService");
        }

        if (timeLimiterRegistry != null) {
            assertThat(timeLimiterRegistry.getAllTimeLimiters())
                .as("TimeLimiter registry should contain emailService")
                .extracting(tl -> tl.getName())
                .contains("emailService");
        }

        if (bulkheadRegistry != null) {
            assertThat(bulkheadRegistry.getAllBulkheads())
                .as("Bulkhead registry should contain emailService")
                .extracting(bh -> bh.getName())
                .contains("emailService");
        }
    }
}
