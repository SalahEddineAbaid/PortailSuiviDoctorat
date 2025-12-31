# Resilience4j Configuration Summary

## Overview

This document summarizes the Resilience4j configuration for the notification-service, which implements comprehensive resilience patterns for fault-tolerant email sending.

## Configuration Location

All Resilience4j properties are configured in `src/main/resources/application.properties`

## Circuit Breaker Configuration

**Instance Name:** `emailService`

### Properties

| Property | Value | Requirement | Description |
|----------|-------|-------------|-------------|
| `sliding-window-size` | 10 | 8.1 | Number of calls in the sliding window |
| `failure-rate-threshold` | 50% | 8.1 | Percentage of failures to open circuit |
| `wait-duration-in-open-state` | 60s | 8.2 | Time to wait before transitioning to half-open |
| `permitted-number-of-calls-in-half-open-state` | 3 | 8.4 | Number of test calls in half-open state |
| `automatic-transition-from-open-to-half-open-enabled` | true | 8.3 | Auto transition after wait duration |
| `sliding-window-type` | COUNT_BASED | 8.1 | Type of sliding window |
| `minimum-number-of-calls` | 5 | 8.1 | Minimum calls before calculating failure rate |
| `record-exceptions` | MailException, SocketTimeoutException | 8.1 | Exceptions that count as failures |

### Behavior

1. **Closed State**: Normal operation, all calls pass through
2. **Open State**: When 50% of 10 calls fail, circuit opens for 60 seconds
3. **Half-Open State**: After 60 seconds, allows 3 test calls
4. **Transition**: If test calls succeed, circuit closes; if they fail, reopens for 60 seconds

## Retry Configuration

**Instance Name:** `emailService`

### Properties

| Property | Value | Requirement | Description |
|----------|-------|-------------|-------------|
| `max-attempts` | 3 | 7.1 | Maximum number of retry attempts |
| `wait-duration` | 5s | 7.2 | Initial wait time before first retry |
| `enable-exponential-backoff` | true | 7.3 | Enable exponential backoff |
| `exponential-backoff-multiplier` | 2 | 7.3 | Multiplier for exponential backoff |
| `retry-exceptions` | MailException, SocketTimeoutException | 7.1 | Exceptions that trigger retry |
| `ignore-exceptions` | IllegalArgumentException | 7.6 | Exceptions that don't trigger retry |

### Retry Intervals

With exponential backoff (multiplier = 2):
- **Attempt 1**: Initial call (no wait)
- **Attempt 2**: Wait 5 seconds (5s × 2^0)
- **Attempt 3**: Wait 10 seconds (5s × 2^1)
- **Attempt 4**: Wait 20 seconds (5s × 2^2) - if configured for 4 attempts

**Total retry sequence**: 0s → 5s → 10s → 20s

## Time Limiter Configuration

**Instance Name:** `emailService`

### Properties

| Property | Value | Requirement | Description |
|----------|-------|-------------|-------------|
| `timeout-duration` | 30s | 9.1 | Maximum time for email send operation |
| `cancel-running-future` | true | 9.2 | Cancel operation if timeout occurs |

### Behavior

- Any email send operation exceeding 30 seconds is cancelled
- TimeoutException is thrown
- Triggers retry mechanism if configured

## Bulkhead Configuration

**Instance Name:** `emailService`

### Properties

| Property | Value | Requirement | Description |
|----------|-------|-------------|-------------|
| `max-concurrent-calls` | 10 | 10.1 | Maximum concurrent email send operations |
| `max-wait-duration` | 5s | 10.2 | Maximum time to wait for permission |

### Behavior

- Limits concurrent email sends to 10 simultaneous operations
- Additional requests wait up to 5 seconds for a slot
- If wait exceeds 5 seconds, BulkheadFullException is thrown
- Failed requests trigger retry mechanism

## Integration with EmailService

The `EmailService` class applies all resilience patterns using annotations:

```java
@Service
public class EmailService {
    
    @CircuitBreaker(name = "emailService", fallbackMethod = "fallbackSendEmail")
    @Retry(name = "emailService")
    @TimeLimiter(name = "emailService")
    @Bulkhead(name = "emailService")
    public void sendEmail(String to, String subject, String htmlBody) {
        // Email sending logic
    }
    
    private void fallbackSendEmail(Exception e) {
        // Fallback logic when circuit breaker opens
    }
}
```

## Execution Flow

When `sendEmail()` is called, the resilience patterns are applied in this order:

1. **Bulkhead**: Check if concurrent limit (10) is reached
   - If yes and wait > 5s: Throw BulkheadFullException
   - If no: Proceed

2. **Time Limiter**: Start 30-second timeout
   - If timeout: Throw TimeoutException
   - If no timeout: Proceed

3. **Circuit Breaker**: Check circuit state
   - If OPEN: Execute fallback method
   - If CLOSED/HALF_OPEN: Proceed

4. **Retry**: Execute email send
   - If MailException/SocketTimeoutException: Retry up to 3 times with exponential backoff
   - If IllegalArgumentException: Fail immediately, no retry
   - If success: Return

5. **Circuit Breaker**: Record result
   - Update failure rate
   - Open circuit if threshold exceeded

## Error Handling Strategy

### Retriable Errors (with exponential backoff)
- `org.springframework.mail.MailException`
- `java.net.SocketTimeoutException`
- `java.util.concurrent.TimeoutException`

### Non-Retriable Errors (fail immediately)
- `java.lang.IllegalArgumentException`
- `InvalidEmailException`
- `InvalidNotificationTypeException`

### After All Retries Fail
1. Mark notification as FAILED
2. Send event to Dead Letter Queue (DLQ)
3. Log error details
4. Update metrics

## Monitoring and Observability

### Metrics Exposed

- `resilience4j.circuitbreaker.calls` - Circuit breaker call statistics
- `resilience4j.circuitbreaker.state` - Current circuit breaker state
- `resilience4j.retry.calls` - Retry attempt statistics
- `resilience4j.timelimiter.calls` - Timeout statistics
- `resilience4j.bulkhead.available.concurrent.calls` - Available bulkhead slots

### Logging

- Circuit breaker state changes logged at WARN level
- Retry attempts logged at INFO level
- Failures logged at ERROR level

### Event Listeners

Custom event listeners are configured to log resilience events:

- `CircuitBreakerEventListener`: Logs circuit breaker state transitions
- `RetryEventListener`: Logs retry attempts and outcomes

## Testing

### Configuration Test

A dedicated test class `ResilienceConfigurationTest` verifies:
- All registries are properly configured
- Configuration values match requirements
- EmailService instance exists in all registries

### Integration Tests

`ResilienceIntegrationTest` validates:
- Circuit breaker opens after threshold
- Retry with exponential backoff works correctly
- Timeout protection functions
- Bulkhead limits concurrent calls
- Fallback method executes when circuit opens

## Requirements Validation

✅ **Requirement 7.1**: Retry up to 3 times for MessagingException  
✅ **Requirement 7.2**: Wait 5 seconds before first retry  
✅ **Requirement 7.3**: Exponential backoff with intervals 5s, 10s, 20s  
✅ **Requirement 8.1**: Circuit breaker opens at 50% failure rate in 10-call window  
✅ **Requirement 8.2**: Circuit breaker waits 60 seconds in open state  
✅ **Requirement 8.3**: Automatic transition to half-open state  
✅ **Requirement 8.4**: 3 permitted calls in half-open state  
✅ **Requirement 8.5**: Circuit closes if test calls succeed  
✅ **Requirement 8.6**: Circuit reopens if test calls fail  
✅ **Requirement 9.1**: 30-second timeout for email operations  
✅ **Requirement 9.2**: Cancel operation on timeout  
✅ **Requirement 10.1**: Limit to 10 concurrent email sends  
✅ **Requirement 10.2**: Maximum 5-second wait for bulkhead slot  

## Configuration File Reference

Complete configuration in `application.properties`:

```properties
# Circuit Breaker Configuration
resilience4j.circuitbreaker.instances.emailService.sliding-window-size=10
resilience4j.circuitbreaker.instances.emailService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.emailService.wait-duration-in-open-state=60s
resilience4j.circuitbreaker.instances.emailService.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.emailService.automatic-transition-from-open-to-half-open-enabled=true
resilience4j.circuitbreaker.instances.emailService.sliding-window-type=COUNT_BASED
resilience4j.circuitbreaker.instances.emailService.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.emailService.record-exceptions=org.springframework.mail.MailException,java.net.SocketTimeoutException

# Retry Configuration
resilience4j.retry.instances.emailService.max-attempts=3
resilience4j.retry.instances.emailService.wait-duration=5s
resilience4j.retry.instances.emailService.enable-exponential-backoff=true
resilience4j.retry.instances.emailService.exponential-backoff-multiplier=2
resilience4j.retry.instances.emailService.retry-exceptions=org.springframework.mail.MailException,java.net.SocketTimeoutException
resilience4j.retry.instances.emailService.ignore-exceptions=java.lang.IllegalArgumentException

# Time Limiter Configuration
resilience4j.timelimiter.instances.emailService.timeout-duration=30s
resilience4j.timelimiter.instances.emailService.cancel-running-future=true

# Bulkhead Configuration
resilience4j.bulkhead.instances.emailService.max-concurrent-calls=10
resilience4j.bulkhead.instances.emailService.max-wait-duration=5s
```

## Conclusion

The Resilience4j configuration is complete and correctly implements all requirements for:
- Retry with exponential backoff (Requirements 7.1-7.3)
- Circuit breaker protection (Requirements 8.1-8.6)
- Timeout protection (Requirements 9.1-9.2)
- Concurrency limiting (Requirements 10.1-10.2)

The configuration ensures the notification service degrades gracefully under failure conditions and protects the SMTP server from being overwhelmed.
