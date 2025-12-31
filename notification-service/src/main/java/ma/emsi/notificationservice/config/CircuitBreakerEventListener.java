package ma.emsi.notificationservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Event listener for Circuit Breaker state changes.
 * Logs circuit breaker events at WARN level for monitoring and alerting.
 * 
 * Requirement 13.4: WHEN the circuit breaker opens THEN log at WARN level
 */
@Component
@Slf4j
public class CircuitBreakerEventListener {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public CircuitBreakerEventListener(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }
    
    /**
     * Initialize event listeners for all circuit breakers after bean construction.
     */
    @PostConstruct
    public void init() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerEventListeners);
    }
    
    /**
     * Register event listeners for a specific circuit breaker.
     * 
     * @param circuitBreaker the circuit breaker to register listeners for
     */
    private void registerEventListeners(CircuitBreaker circuitBreaker) {
        String circuitBreakerName = circuitBreaker.getName();
        
        // Listen for state transitions (CLOSED -> OPEN, OPEN -> HALF_OPEN, HALF_OPEN -> CLOSED, etc.)
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> logStateTransition(circuitBreakerName, event));
        
        // Listen for successful calls
        circuitBreaker.getEventPublisher()
            .onSuccess(event -> logSuccess(circuitBreakerName, event));
        
        // Listen for error calls
        circuitBreaker.getEventPublisher()
            .onError(event -> logError(circuitBreakerName, event));
        
        log.info("Circuit breaker event listeners registered for: {}", circuitBreakerName);
    }
    
    /**
     * Log circuit breaker state transitions at WARN level.
     * Requirement 13.4: WHEN the circuit breaker opens THEN log at WARN level
     * 
     * @param circuitBreakerName the name of the circuit breaker
     * @param event the state transition event
     */
    private void logStateTransition(String circuitBreakerName, CircuitBreakerOnStateTransitionEvent event) {
        log.warn("=== Circuit Breaker State Transition ===");
        log.warn("Circuit Breaker: {}", circuitBreakerName);
        log.warn("From State: {}", event.getStateTransition().getFromState());
        log.warn("To State: {}", event.getStateTransition().getToState());
        log.warn("Timestamp: {}", event.getCreationTime());
        log.warn("========================================");
        
        // Additional specific logging for critical transitions
        switch (event.getStateTransition().getToState()) {
            case OPEN:
                log.warn("ALERT: Circuit breaker '{}' has OPENED! Email service calls will be rejected for the configured wait duration.", 
                         circuitBreakerName);
                break;
            case HALF_OPEN:
                log.warn("Circuit breaker '{}' transitioned to HALF_OPEN. Testing with limited calls.", 
                         circuitBreakerName);
                break;
            case CLOSED:
                log.info("Circuit breaker '{}' has CLOSED. Normal operation resumed.", 
                         circuitBreakerName);
                break;
            case FORCED_OPEN:
                log.warn("Circuit breaker '{}' has been FORCED OPEN manually.", 
                         circuitBreakerName);
                break;
            default:
                log.debug("Circuit breaker '{}' state: {}", 
                          circuitBreakerName, event.getStateTransition().getToState());
        }
    }
    
    /**
     * Log successful circuit breaker calls at DEBUG level.
     * 
     * @param circuitBreakerName the name of the circuit breaker
     * @param event the success event
     */
    private void logSuccess(String circuitBreakerName, CircuitBreakerOnSuccessEvent event) {
        log.debug("Circuit breaker '{}' - Successful call. Duration: {} ms", 
                  circuitBreakerName, event.getElapsedDuration().toMillis());
    }
    
    /**
     * Log failed circuit breaker calls at DEBUG level.
     * 
     * @param circuitBreakerName the name of the circuit breaker
     * @param event the error event
     */
    private void logError(String circuitBreakerName, CircuitBreakerOnErrorEvent event) {
        log.debug("Circuit breaker '{}' - Failed call. Duration: {} ms, Error: {}", 
                  circuitBreakerName, 
                  event.getElapsedDuration().toMillis(),
                  event.getThrowable().getMessage());
    }
}
