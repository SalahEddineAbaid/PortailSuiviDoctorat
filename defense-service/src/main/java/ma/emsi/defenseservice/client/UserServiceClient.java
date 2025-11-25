package ma.emsi.defenseservice.client;

import ma.emsi.defenseservice.dto.external.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client Feign pour communiquer avec le user-service
 * Note: Le nom doit correspondre au nom enregistré dans Eureka (depuis Config
 * Server: "user-service")
 */
@FeignClient(name = "user-service", path = "/api/users")
public interface UserServiceClient {

    /**
     * Récupérer un utilisateur par son ID
     */
    @GetMapping("/{id}")
    UserDTO getUserById(@PathVariable Long id);

    /**
     * Récupérer le profil de l'utilisateur connecté
     * (nécessite un token JWT dans le header)
     */
    @GetMapping("/profile")
    UserDTO getCurrentUser();
}
