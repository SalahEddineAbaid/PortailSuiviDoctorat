package ma.emsi.notificationservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import ma.emsi.notificationservice.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for JWT authentication and authorization.
 * Tests that REST endpoints are properly secured with JWT tokens.
 * 
 * Requirements: 14.7 - Testing REST endpoints to verify authentication and authorization rules
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb_security",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mySecretKeyForJWTTokenGenerationAndValidation12345678901234567890",
    "jwt.expiration=86400000",
    "logging.level.ma.emsi.notificationservice.security=DEBUG"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private String adminToken;
    private String userToken;
    private String invalidToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        
        // Generate test JWT tokens
        adminToken = generateToken("admin@emsi.ma", "ROLE_ADMIN");
        userToken = generateToken("user@emsi.ma", "ROLE_USER");
        invalidToken = "invalid.jwt.token";
        expiredToken = generateExpiredToken("expired@emsi.ma", "ROLE_USER");
    }

    private String generateToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    private String generateExpiredToken(String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("role", role);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(email)
            .setIssuedAt(new Date(System.currentTimeMillis() - 2 * jwtExpiration))
            .setExpiration(new Date(System.currentTimeMillis() - jwtExpiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    @Test
    void testGetAllNotifications_WithoutToken_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllNotifications_WithInvalidToken_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + invalidToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllNotifications_WithExpiredToken_Returns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + expiredToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllNotifications_WithValidAdminToken_Returns200() throws Exception {
        // Arrange
        Notification notification = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("test@emsi.ma")
            .sujet("Test notification")
            .messageTexte("Test message")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.SENT)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(0)
            .dateCreation(LocalDateTime.now())
            .dateEnvoi(LocalDateTime.now())
            .build();
        
        notificationRepository.save(notification);

        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testGetAllNotifications_WithValidUserToken_Returns403() throws Exception {
        // Act & Assert - Regular users should not access all notifications
        mockMvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetNotificationById_WithValidToken_Returns200() throws Exception {
        // Arrange
        Notification notification = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("admin@emsi.ma")
            .sujet("Test notification")
            .messageTexte("Test message")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.SENT)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(0)
            .dateCreation(LocalDateTime.now())
            .dateEnvoi(LocalDateTime.now())
            .build();
        
        notification = notificationRepository.save(notification);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/" + notification.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(notification.getId()))
            .andExpect(jsonPath("$.destinataire").value("admin@emsi.ma"));
    }

    @Test
    void testGetUserNotifications_WithMatchingEmail_Returns200() throws Exception {
        // Arrange
        Notification notification = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("user@emsi.ma")
            .sujet("Test notification")
            .messageTexte("Test message")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.SENT)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(0)
            .dateCreation(LocalDateTime.now())
            .dateEnvoi(LocalDateTime.now())
            .build();
        
        notificationRepository.save(notification);

        // Act & Assert - User can access their own notifications
        mockMvc.perform(get("/api/notifications/user/user@emsi.ma")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testGetUserNotifications_WithNonMatchingEmail_Returns403() throws Exception {
        // Act & Assert - User cannot access other users' notifications
        mockMvc.perform(get("/api/notifications/user/other@emsi.ma")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetUserNotifications_AdminCanAccessAnyUser_Returns200() throws Exception {
        // Arrange
        Notification notification = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("anyuser@emsi.ma")
            .sujet("Test notification")
            .messageTexte("Test message")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.SENT)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(0)
            .dateCreation(LocalDateTime.now())
            .dateEnvoi(LocalDateTime.now())
            .build();
        
        notificationRepository.save(notification);

        // Act & Assert - Admin can access any user's notifications
        mockMvc.perform(get("/api/notifications/user/anyuser@emsi.ma")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testGetNotificationsByStatus_WithAdminToken_Returns200() throws Exception {
        // Arrange
        Notification notification = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("test@emsi.ma")
            .sujet("Test notification")
            .messageTexte("Test message")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.FAILED)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(3)
            .dateCreation(LocalDateTime.now())
            .erreurMessage("Test error")
            .build();
        
        notificationRepository.save(notification);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/status/FAILED")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testGetNotificationsByStatus_WithUserToken_Returns403() throws Exception {
        // Act & Assert - Regular users cannot query by status
        mockMvc.perform(get("/api/notifications/status/FAILED")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetNotificationStats_WithAdminToken_Returns200() throws Exception {
        // Arrange
        Notification notification1 = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("test1@emsi.ma")
            .sujet("Test 1")
            .messageTexte("Test")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.SENT)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(0)
            .dateCreation(LocalDateTime.now())
            .dateEnvoi(LocalDateTime.now())
            .build();
        
        Notification notification2 = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("test2@emsi.ma")
            .sujet("Test 2")
            .messageTexte("Test")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.FAILED)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(3)
            .dateCreation(LocalDateTime.now())
            .erreurMessage("Error")
            .build();
        
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/stats")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.sent").value(1))
            .andExpect(jsonPath("$.failed").value(1));
    }

    @Test
    void testGetNotificationStats_WithUserToken_Returns403() throws Exception {
        // Act & Assert - Regular users cannot access stats
        mockMvc.perform(get("/api/notifications/stats")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testRetryNotification_WithAdminToken_Returns200() throws Exception {
        // Arrange
        Notification notification = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("test@emsi.ma")
            .sujet("Test retry")
            .messageTexte("Test message")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.FAILED)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(3)
            .dateCreation(LocalDateTime.now())
            .erreurMessage("Previous error")
            .build();
        
        notification = notificationRepository.save(notification);

        // Act & Assert
        mockMvc.perform(post("/api/notifications/" + notification.getId() + "/retry")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    void testRetryNotification_WithUserToken_Returns403() throws Exception {
        // Arrange
        Notification notification = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("test@emsi.ma")
            .sujet("Test retry")
            .messageTexte("Test message")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.FAILED)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(3)
            .dateCreation(LocalDateTime.now())
            .erreurMessage("Previous error")
            .build();
        
        notification = notificationRepository.save(notification);

        // Act & Assert - Regular users cannot retry notifications
        mockMvc.perform(post("/api/notifications/" + notification.getId() + "/retry")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetFailedNotifications_WithAdminToken_Returns200() throws Exception {
        // Arrange
        Notification notification = Notification.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("test@emsi.ma")
            .sujet("Failed notification")
            .messageTexte("Test message")
            .messageHtml("<html>Test</html>")
            .statut(StatutNotification.FAILED)
            .priorite(PrioriteNotification.NORMALE)
            .nombreTentatives(3)
            .dateCreation(LocalDateTime.now())
            .erreurMessage("Test error")
            .build();
        
        notificationRepository.save(notification);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/failed")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testGetFailedNotifications_WithUserToken_Returns403() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/notifications/failed")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }
}
