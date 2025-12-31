package ma.emsi.notificationservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SecurityConfig.
 * Tests that security rules are properly applied.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testPublicEndpoint_Actuator_ReturnsOk() throws Exception {
        // When: accessing actuator health endpoint without authentication
        // Then: should return 200 OK (public endpoint)
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpoint_WithoutAuth_ReturnsUnauthorized() throws Exception {
        // When: accessing protected endpoint without JWT token
        // Then: should return 401 Unauthorized
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testProtectedEndpoint_WithInvalidToken_ReturnsUnauthorized() throws Exception {
        // When: accessing protected endpoint with invalid JWT token
        // Then: should return 401 Unauthorized
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }
}
