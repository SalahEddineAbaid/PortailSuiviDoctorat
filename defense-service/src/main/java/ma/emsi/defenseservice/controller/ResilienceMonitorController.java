package ma.emsi.defenseservice.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller pour monitorer l'état de Resilience4j
 */
@RestController
@RequestMapping("/api/resilience")
public class ResilienceMonitorController {

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Récupère l'état du Circuit Breaker pour le user-service
     */
    @GetMapping("/circuit-breaker/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");

        Map<String, Object> status = new HashMap<>();
        status.put("name", circuitBreaker.getName());
        status.put("state", circuitBreaker.getState().toString());
        status.put("metrics", getMetrics(circuitBreaker));

        return ResponseEntity.ok(status);
    }

    /**
     * Récupère les métriques détaillées du Circuit Breaker
     */
    private Map<String, Object> getMetrics(CircuitBreaker circuitBreaker) {
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("failureRate", metrics.getFailureRate() + "%");
        metricsMap.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
        metricsMap.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
        metricsMap.put("numberOfNotPermittedCalls", metrics.getNumberOfNotPermittedCalls());
        metricsMap.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());

        return metricsMap;
    }

    /**
     * Réinitialise le Circuit Breaker (utile pour les tests)
     */
    @GetMapping("/circuit-breaker/reset")
    public ResponseEntity<String> resetCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        circuitBreaker.reset();

        return ResponseEntity.ok("Circuit Breaker 'userService' réinitialisé avec succès");
    }
}
