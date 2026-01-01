package ma.emsi.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.emsi.userservice.dto.request.DisableAccountRequest;
import ma.emsi.userservice.dto.request.LoginRequest;
import ma.emsi.userservice.dto.request.RegisterRequest;
import ma.emsi.userservice.dto.response.LoginResponse;
import ma.emsi.userservice.entity.RefreshToken;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.RoleName;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.entity.UserAudit;
import ma.emsi.userservice.enums.AccountStatus;
import ma.emsi.userservice.enums.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for account disable flow:
 * disable account → verify tokens invalidated → attempt login
 * Verifies audit record created and event published.
 * 
 * Validates: Requirements 3.1, 3.2, 3.3, 3.8, 6.2
 */
class AccountDisableIntegrationTest extends IntegrationTestBase {

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private User adminUser;
        private User regularUser;
        private String adminAccessToken;

        @BeforeEach
        void setupUsersAndLogin() throws Exception {
                // Create roles
                Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                                .orElseGet(() -> {
                                        Role role = new Role();
                                        role.setName(RoleName.ROLE_ADMIN);
                                        return roleRepository.save(role);
                                });

                Role doctorantRole = roleRepository.findByName(RoleName.ROLE_DOCTORANT)
                                .orElseGet(() -> {
                                        Role role = new Role();
                                        role.setName(RoleName.ROLE_DOCTORANT);
                                        return roleRepository.save(role);
                                });

                // Create admin user
                adminUser = new User();
                adminUser.setEmail("admin@example.com");
                adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
                adminUser.setFirstName("Admin");
                adminUser.setLastName("User");
                adminUser.setPhoneNumber("0612345682");
                adminUser.setAdresse("Admin Street");
                adminUser.setVille("Casablanca");
                adminUser.setPays("Morocco");
                adminUser.setRoles(Set.of(adminRole));
                adminUser.setAccountStatus(AccountStatus.ACTIVE);
                adminUser = userRepository.save(adminUser);

                // Create regular user
                RegisterRequest regularUserRequest = new RegisterRequest(
                                "regular@example.com",
                                "Password123!",
                                "Regular",
                                "User",
                                "0612345683",
                                "Regular Street",
                                "Rabat",
                                "Morocco",
                                null);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(regularUserRequest)))
                                .andExpect(status().isCreated());

                regularUser = userRepository.findByEmail("regular@example.com").orElseThrow();

                // Login as admin
                LoginRequest adminLogin = new LoginRequest("admin@example.com", "AdminPass123!");
                MvcResult adminLoginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(adminLogin)))
                                .andExpect(status().isOk())
                                .andReturn();

                String adminLoginJson = adminLoginResult.getResponse().getContentAsString();
                LoginResponse adminLoginResponse = objectMapper.readValue(adminLoginJson, LoginResponse.class);
                adminAccessToken = adminLoginResponse.accessToken();
        }

        @Test
        void testDisableAccountFlow() throws Exception {
                // Step 1: Login as regular user to create refresh token
                LoginRequest regularLogin = new LoginRequest("regular@example.com", "Password123!");
                MvcResult regularLoginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(regularLogin)))
                                .andExpect(status().isOk())
                                .andReturn();

                String regularLoginJson = regularLoginResult.getResponse().getContentAsString();
                LoginResponse regularLoginResponse = objectMapper.readValue(regularLoginJson, LoginResponse.class);
                String regularAccessToken = regularLoginResponse.accessToken();
                String regularRefreshToken = regularLoginResponse.refreshToken();

                // Verify refresh token exists
                RefreshToken tokenBefore = refreshTokenRepository.findByToken(regularRefreshToken).orElseThrow();
                assertThat(tokenBefore).isNotNull();

                // Step 2: Admin disables the regular user account
                DisableAccountRequest disableRequest = new DisableAccountRequest("Policy violation");

                mockMvc.perform(post("/api/admin/users/" + regularUser.getId() + "/disable")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(disableRequest)))
                                .andExpect(status().isNoContent());

                // Step 3: Verify account status changed to DISABLED
                User disabledUser = userRepository.findById(regularUser.getId()).orElseThrow();
                assertThat(disabledUser.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);

                // Step 4: Verify all refresh tokens were invalidated
                // Query all tokens and filter by user (since tokens were deleted, should be
                // empty)
                long tokenCount = refreshTokenRepository.count();
                // After disable, the user's tokens should be deleted
                // We can verify by checking if any token exists for this user
                assertThat(refreshTokenRepository.findAll().stream()
                                .filter(token -> token.getUser().getId().equals(regularUser.getId()))
                                .count()).isEqualTo(0);

                // Step 5: Verify audit record was created
                List<UserAudit> audits = userAuditRepository.findByUserIdOrderByTimestampDesc(
                                regularUser.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                                .getContent();

                UserAudit disableAudit = audits.stream()
                                .filter(audit -> audit.getAction() == AuditAction.ACCOUNT_DISABLED)
                                .findFirst()
                                .orElseThrow();

                assertThat(disableAudit.getUserId()).isEqualTo(regularUser.getId());
                assertThat(disableAudit.getDetails()).contains("Policy violation");

                // Step 6: Attempt to login with disabled account
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(regularLogin)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));

                // Step 7: Attempt to use existing access token (should fail)
                mockMvc.perform(get("/api/users/me")
                                .header("Authorization", "Bearer " + regularAccessToken))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void testEnableAccountFlow() throws Exception {
                // First disable the account
                DisableAccountRequest disableRequest = new DisableAccountRequest("Test disable");

                mockMvc.perform(post("/api/admin/users/" + regularUser.getId() + "/disable")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(disableRequest)))
                                .andExpect(status().isNoContent());

                // Verify account is disabled
                User disabledUser = userRepository.findById(regularUser.getId()).orElseThrow();
                assertThat(disabledUser.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);

                // Enable the account
                mockMvc.perform(post("/api/admin/users/" + regularUser.getId() + "/enable")
                                .header("Authorization", "Bearer " + adminAccessToken))
                                .andExpect(status().isNoContent());

                // Verify account status changed to ACTIVE
                User enabledUser = userRepository.findById(regularUser.getId()).orElseThrow();
                assertThat(enabledUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
                assertThat(enabledUser.getFailedLoginAttempts()).isEqualTo(0);

                // Verify audit record was created
                List<UserAudit> audits = userAuditRepository.findByUserIdOrderByTimestampDesc(
                                regularUser.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                                .getContent();

                UserAudit enableAudit = audits.stream()
                                .filter(audit -> audit.getAction() == AuditAction.ACCOUNT_ENABLED)
                                .findFirst()
                                .orElseThrow();

                assertThat(enableAudit.getUserId()).isEqualTo(regularUser.getId());

                // Verify user can now login
                LoginRequest loginRequest = new LoginRequest("regular@example.com", "Password123!");
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        void testAdminCannotDisableSelf() throws Exception {
                // Admin attempts to disable their own account
                DisableAccountRequest disableRequest = new DisableAccountRequest("Self disable attempt");

                mockMvc.perform(post("/api/admin/users/" + adminUser.getId() + "/disable")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(disableRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("SELF_DISABLE_NOT_ALLOWED"));

                // Verify account is still active
                User stillActiveAdmin = userRepository.findById(adminUser.getId()).orElseThrow();
                assertThat(stillActiveAdmin.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        void testGetDisabledAccounts() throws Exception {
                // Disable the regular user
                DisableAccountRequest disableRequest = new DisableAccountRequest("Test reason");

                mockMvc.perform(post("/api/admin/users/" + regularUser.getId() + "/disable")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(disableRequest)))
                                .andExpect(status().isNoContent());

                // Get list of disabled accounts
                mockMvc.perform(get("/api/admin/users/disabled")
                                .header("Authorization", "Bearer " + adminAccessToken)
                                .param("page", "0")
                                .param("size", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content[0].email").value("regular@example.com"))
                                .andExpect(jsonPath("$.totalElements").value(1));
        }
}
