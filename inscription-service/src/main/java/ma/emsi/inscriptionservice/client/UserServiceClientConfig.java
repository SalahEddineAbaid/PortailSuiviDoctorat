package ma.emsi.inscriptionservice.client;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.UserDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for UserServiceClient with circuit breaker and retry mechanisms
 * Provides resilience patterns for external service calls
 */
@Configuration
@Slf4j
public class UserServiceClientConfig {

    /**
     * Configure Feign request options
     * Sets connection and read timeouts
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5000,  // Connect timeout in milliseconds
            5000   // Read timeout in milliseconds
        );
    }

    /**
     * Configure Feign retry mechanism
     * Retries failed requests with exponential backoff
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            1000,  // Initial interval (1 second)
            3000,  // Max interval (3 seconds)
            3      // Max attempts
        );
    }

    /**
     * Configure Feign logging level
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * Fallback method for getUserById
     * Returns a default UserDTO when the service is unavailable
     */
    public UserDTO getUserByIdFallback(Long userId, Throwable ex) {
        log.error("Failed to fetch user {}: {}. Using fallback.", userId, ex.getMessage());
        return UserDTO.builder()
            .id(userId)
            .firstName("Unknown")
            .lastName("User")
            .email("unknown@emsi.ma")
            .role("UNKNOWN")
            .active(false)
            .build();
    }

    /**
     * Fallback method for getDirectorInfo
     * Returns a default director UserDTO when the service is unavailable
     */
    public UserDTO getDirectorInfoFallback(Long directeurId, Throwable ex) {
        log.error("Failed to fetch director {}: {}. Using fallback.", directeurId, ex.getMessage());
        return UserDTO.builder()
            .id(directeurId)
            .firstName("Directeur")
            .lastName("Non disponible")
            .email("directeur@emsi.ma")
            .role("DIRECTEUR")
            .active(false)
            .build();
    }

    /**
     * Fallback method for getStudentInfo
     * Returns a default student UserDTO when the service is unavailable
     */
    public UserDTO getStudentInfoFallback(Long doctorantId, Throwable ex) {
        log.error("Failed to fetch student {}: {}. Using fallback.", doctorantId, ex.getMessage());
        return UserDTO.builder()
            .id(doctorantId)
            .firstName("Étudiant")
            .lastName("Non disponible")
            .email("etudiant@emsi.ma")
            .role("DOCTORANT")
            .active(false)
            .build();
    }
}
