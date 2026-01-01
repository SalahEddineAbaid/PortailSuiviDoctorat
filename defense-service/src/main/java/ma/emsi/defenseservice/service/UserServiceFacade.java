package ma.emsi.defenseservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import ma.emsi.defenseservice.client.UserServiceClient;
import ma.emsi.defenseservice.dto.external.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;

/**
 * Facade pour les appels au user-service avec Resilience4j
 * Gère Circuit Breaker, Retry et Fallback
 */
@Service
public class UserServiceFacade {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceFacade.class);

    @Autowired
    private UserServiceClient userServiceClient;

    /**
     * Récupère un utilisateur avec Circuit Breaker et Retry
     * 
     * @param userId ID de l'utilisateur
     * @return UserDTO ou fallback si le service est indisponible
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "userService")
    public UserDTO getUserById(Long userId) {
        logger.info("🔍 Appel au user-service pour l'utilisateur ID: {}", userId);
        UserDTO user = userServiceClient.getUserById(userId);
        logger.info("✅ Utilisateur récupéré: {} {}", user.getFirstName(), user.getLastName());
        return user;
    }

    /**
     * Méthode de fallback en cas d'échec du user-service
     * Retourne un utilisateur par défaut avec des informations minimales
     */
    private UserDTO getUserByIdFallback(Long userId, Exception e) {
        logger.error("❌ Fallback activé pour l'utilisateur ID: {}. Erreur: {}",
                userId, e.getMessage());

        UserDTO fallbackUser = new UserDTO();
        fallbackUser.setId(userId);
        fallbackUser.setFirstName("Utilisateur");
        fallbackUser.setLastName("Indisponible");
        fallbackUser.setEmail("unavailable@system.local");
        fallbackUser.setPhoneNumber("N/A");
        fallbackUser.setAdresse("N/A");
        fallbackUser.setVille("N/A");
        fallbackUser.setPays("N/A");
        fallbackUser.setRoles(new HashSet<>());

        return fallbackUser;
    }

    /**
     * Vérifie si un utilisateur existe et a un rôle spécifique
     * Avec gestion d'erreur améliorée
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "validateUserRoleFallback")
    @Retry(name = "userService")
    public boolean validateUserRole(Long userId, String requiredRole) {
        logger.info("🔍 Validation du rôle {} pour l'utilisateur ID: {}", requiredRole, userId);

        try {
            UserDTO user = userServiceClient.getUserById(userId);

            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                logger.warn("⚠️ L'utilisateur {} n'a aucun rôle assigné", userId);
                return false;
            }

            boolean hasRole = user.getRoles().contains(requiredRole);
            logger.info("✅ Validation du rôle {}: {}", requiredRole, hasRole);
            return hasRole;

        } catch (Exception e) {
            logger.error("❌ Erreur lors de la validation du rôle: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Fallback pour la validation de rôle
     * Retourne false par défaut pour éviter les autorisations non vérifiées
     */
    private boolean validateUserRoleFallback(Long userId, String requiredRole, Exception e) {
        logger.error("❌ Fallback activé pour la validation du rôle {} de l'utilisateur ID: {}. Erreur: {}",
                requiredRole, userId, e.getMessage());

        // Par sécurité, on refuse l'accès si le service est indisponible
        return false;
    }

    /**
     * Récupère le profil de l'utilisateur connecté avec Circuit Breaker et Retry
     * 
     * @return UserDTO ou fallback si le service est indisponible
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "getCurrentUserFallback")
    @Retry(name = "userService")
    public UserDTO getCurrentUser() {
        logger.info("🔍 Appel au user-service pour l'utilisateur connecté");
        UserDTO user = userServiceClient.getCurrentUser();
        logger.info("✅ Utilisateur connecté récupéré: {} {}", user.getFirstName(), user.getLastName());
        return user;
    }

    /**
     * Méthode de fallback pour getCurrentUser en cas d'échec du user-service
     * Retourne un utilisateur par défaut avec des informations minimales
     */
    private UserDTO getCurrentUserFallback(Exception e) {
        logger.error("❌ Fallback activé pour l'utilisateur connecté. Erreur: {}", e.getMessage());

        UserDTO fallbackUser = new UserDTO();
        fallbackUser.setId(0L);
        fallbackUser.setFirstName("Utilisateur");
        fallbackUser.setLastName("Connecté");
        fallbackUser.setEmail("current@system.local");
        fallbackUser.setPhoneNumber("N/A");
        fallbackUser.setAdresse("N/A");
        fallbackUser.setVille("N/A");
        fallbackUser.setPays("N/A");
        fallbackUser.setRoles(new HashSet<>());

        return fallbackUser;
    }
}
