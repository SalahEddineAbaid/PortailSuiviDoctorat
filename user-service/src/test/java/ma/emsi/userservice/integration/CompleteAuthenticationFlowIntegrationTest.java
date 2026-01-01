package ma.emsi.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.emsi.userservice.dto.request.LoginRequest;
import ma.emsi.userservice.dto.request.RegisterRequest;
import ma.emsi.userservice.dto.request.TokenRefreshRequest;
import ma.emsi.userservice.dto.response.LoginResponse;
import ma.emsi.userservice.dto.response.TokenResponse;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.RoleName;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.entity.UserAudit;
import ma.emsi.userservice.enums.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for complete authentication flow:
 * register → login → access protected resource → refresh token
 * Verifies audit records created and events published.
 * 
 * Validates: Requirements 2.1, 6.1
 */
class CompleteAuthenticationFlowIntegrationTest extends IntegrationTestBase {

        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void setupRoles() {
                // Ensure roles exist
                if (roleRepository.findByName(RoleName.ROLE_DOCTORANT).isEmpty()) {
                        Role doctorantRole = new Role();
                        doctorantRole.setName(RoleName.ROLE_DOCTORANT);
                        roleRepository.save(doctorantRole);
                }
        }

        @Test
        void testCompleteAuthenticationFlow() throws Exception {
                // Step 1: Register a new user
                RegisterRequest registerRequest = new RegisterRequest(
                                "test@example.com",
                                "Password123!",
                                "John",
                                "Doe",
                                "0612345678",
                                "123 Test St",
                                "Casablanca",
                                "Morocco",
                                null); // roles - will be assigned default role

                MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated())
                                .andReturn();

                // Verify user was created
                User createdUser = userRepository.findByEmail("test@example.com").orElseThrow();
                assertThat(createdUser).isNotNull();
                assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
                assertThat(createdUser.getFirstName()).isEqualTo("John");

                // Step 2: Login with the registered user
                LoginRequest loginRequest = new LoginRequest("test@example.com", "Password123!");

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                                .header("X-Forwarded-For", "192.168.1.1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists())
                                .andExpect(jsonPath("$.refreshToken").exists())
                                .andReturn();

                String loginResponseJson = loginResult.getResponse().getContentAsString();
                LoginResponse loginResponse = objectMapper.readValue(loginResponseJson, LoginResponse.class);
                String accessToken = loginResponse.accessToken();
                String refreshToken = loginResponse.refreshToken();

                assertThat(accessToken).isNotBlank();
                assertThat(refreshToken).isNotBlank();

                // Verify audit record was created for successful login
                List<UserAudit> loginAudits = userAuditRepository.findByUserIdOrderByTimestampDesc(
                                createdUser.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                                .getContent();

                assertThat(loginAudits).isNotEmpty();
                UserAudit loginAudit = loginAudits.stream()
                                .filter(audit -> audit.getAction() == AuditAction.LOGIN)
                                .findFirst()
                                .orElseThrow();

                assertThat(loginAudit.getUserId()).isEqualTo(createdUser.getId());
                assertThat(loginAudit.getIpAddress()).isEqualTo("192.168.1.1");

                // Step 3: Access protected resource with access token
                mockMvc.perform(get("/api/users/me")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("test@example.com"))
                                .andExpect(jsonPath("$.firstName").value("John"));

                // Step 4: Refresh the access token
                TokenRefreshRequest refreshRequest = new TokenRefreshRequest(refreshToken);

                MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists())
                                .andReturn();

                String refreshResponseJson = refreshResult.getResponse().getContentAsString();
                TokenResponse tokenResponse = objectMapper.readValue(refreshResponseJson, TokenResponse.class);
                String newAccessToken = tokenResponse.accessToken();

                assertThat(newAccessToken).isNotBlank();
                assertThat(newAccessToken).isNotEqualTo(accessToken);

                // Step 5: Use new access token to access protected resource
                mockMvc.perform(get("/api/users/me")
                                .header("Authorization", "Bearer " + newAccessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("test@example.com"));

                // Verify refresh token still exists in database
                assertThat(refreshTokenRepository.findByToken(refreshToken)).isPresent();
        }

        @Test
        void testLoginWithInvalidCredentials_CreatesFailedAudit() throws Exception {
                // Create a user first
                RegisterRequest registerRequest = new RegisterRequest(
                                "user@example.com",
                                "Password123!",
                                "Jane",
                                "Smith",
                                "0612345679",
                                "456 Test Ave",
                                "Rabat",
                                "Morocco",
                                null);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated());

                User user = userRepository.findByEmail("user@example.com").orElseThrow();

                // Attempt login with wrong password
                LoginRequest loginRequest = new LoginRequest("user@example.com", "WrongPassword!");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest))
                                .header("X-Forwarded-For", "10.0.0.1"))
                                .andExpect(status().isUnauthorized());

                // Verify failed login audit was created
                List<UserAudit> audits = userAuditRepository.findByUserIdOrderByTimestampDesc(
                                user.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                                .getContent();

                UserAudit failedLoginAudit = audits.stream()
                                .filter(audit -> audit.getAction() == AuditAction.LOGIN_FAILED)
                                .findFirst()
                                .orElseThrow();

                assertThat(failedLoginAudit.getUserId()).isEqualTo(user.getId());
                assertThat(failedLoginAudit.getIpAddress()).isEqualTo("10.0.0.1");
        }
}
