package ma.emsi.inscriptionservice.client;

import ma.emsi.inscriptionservice.DTOs.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with the User Service
 * Includes circuit breaker and retry mechanisms for resilience
 */
@FeignClient(name = "USER-SERVICE", path = "/api/users", configuration = UserServiceClientConfig.class)
public interface UserServiceClient {

    /**
     * Fetch user by ID
     * Used for general user information retrieval
     * 
     * @param id User ID
     * @return User details
     */
    @GetMapping("/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);

    /**
     * Fetch user by email
     * Used for user lookup by email address
     * 
     * @param email User email
     * @return User details
     */
    @GetMapping("/email/{email}")
    UserDTO getUserByEmail(@PathVariable("email") String email);

    /**
     * Fetch director information for attestation generation
     * This method is specifically used when generating attestation PDFs
     * Requirements: 2.2
     * 
     * @param directeurId Director's user ID
     * @return Director details including name and contact information
     */
    @GetMapping("/{directeurId}")
    UserDTO getDirectorInfo(@PathVariable("directeurId") Long directeurId);

    /**
     * Fetch student information for dashboard
     * This method is specifically used when building student dashboards
     * Requirements: 5.1
     * 
     * @param doctorantId Student's user ID
     * @return Student details including name, email, and enrollment information
     */
    @GetMapping("/{doctorantId}")
    UserDTO getStudentInfo(@PathVariable("doctorantId") Long doctorantId);
}
