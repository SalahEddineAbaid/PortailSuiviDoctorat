package ma.emsi.defenseservice.controller;

import ma.emsi.defenseservice.client.UserServiceClient;
import ma.emsi.defenseservice.dto.external.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller de test pour vérifier la communication avec user-service
 */
@RestController
@RequestMapping("/api/defense-service/test")
public class TestController {

    @Autowired
    private UserServiceClient userServiceClient;

    /**
     * Test de connectivité avec user-service
     */
    @GetMapping("/user/{id}")
    public ResponseEntity<?> testGetUser(@PathVariable Long id) {
        try {
            UserDTO user = userServiceClient.getUserById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Communication avec user-service réussie");
            response.put("user", user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur de communication avec user-service");
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getName());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Endpoint de santé
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "defense-service");
        return ResponseEntity.ok(response);
    }
}
