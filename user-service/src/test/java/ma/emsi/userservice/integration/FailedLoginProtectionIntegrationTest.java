package ma.emsi.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.emsi.userservice.dto.request.LoginRequest;
import ma.emsi.userservice.dto.request.RegisterRequest;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for failed login protection:
 * multiple failed logins → account locked → auto-unlock
 * Verifies audit records created.
 * 
 * Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5
 */
class FailedLoginProtectionIntegrationTest extends IntegrationTestBase {

        @Autowired
        private ObjectMapper objectMapper;

        private User testUser;

        @BeforeEach
        void setupUser() throws Exception {
                // Ensure roles exist
                if (roleRepository.findByName(RoleName.ROLE_DOCTORANT).isEmpty()) {
                        Role doctorantRole = new Role();
                        doctorantRole.setName(RoleName.ROLE_DOCTORANT);
                        roleRepository.save(doctorantRole);
                }

                // Register a test user
                RegisterRequest registerRequest = new RegisterRequest(
                                "testuser@example.com",
                                "CorrectPassword123!",
                                "Test",
                                "User",
                                "0612345684",
                                "Test Address",
                                "Casablanca",
                                "Morocco",
                                null);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated());

                testUser = userRepository.findByEmail("testuser@example.com").orElseThrow();
        }

        @Test
        void testAccountLocksAfterMaxFailedAttempts() throws Exception {
                LoginRequest wrongPasswordRequest = new LoginRequest("testuser@example.com", "WrongPassword!");

                // Attempt 1-4: Failed logins (should not lock yet)
                for (int i = 0; i < 4; i++) {
                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(wrongPasswordRequest))
                                        .header("X-Forwarded-For", "192.168.1.100"))
                                        .andExpect(status().isUnauthorized());

                        // Verify account is not locked yet
                        User user = userRepository.findById(testUser.getId()).orElseThrow();
                        assertThat(user.getFailedLoginAttempts()).isEqualTo(i + 1);
                        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
                }

                // Attempt 5: Should lock the account
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongPasswordRequest))
                                .header("X-Forwarded-For", "192.168.1.100"))
                                .andExpect(status().isUnauthorized());

                // Verify account is now locked
                User lockedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(lockedUser.getFailedLoginAttempts()).isEqualTo(5);
                assertThat(lockedUser.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);
                assertThat(lockedUser.getLockoutExpiration()).isNotNull();
                assertThat(lockedUser.getLockoutExpiration()).isAfter(LocalDateTime.now());

                // Verify failed login audit records were created
                List<UserAudit> audits = userAuditRepository.findByUserIdOrderByTimestampDesc(
                                testUser.getId(), org.springframework.data.domain.PageRequest.of(0, 20))
                                .getContent();

                long failedLoginCount = audits.stream()
                                .filter(audit -> audit.getAction() == AuditAction.LOGIN_FAILED)
                                .count();

                assertThat(failedLoginCount).isEqualTo(5);

                // Verify account locked audit record
                UserAudit lockedAudit = audits.stream()
                                .filter(audit -> audit.getAction() == AuditAction.ACCOUNT_LOCKED)
                                .findFirst()
                                .orElseThrow();

                assertThat(lockedAudit.getUserId()).isEqualTo(testUser.getId());
        }

        @Test
        void testLockedAccountRejectsLogin() throws Exception {
                // Lock the account by failing 5 times
                LoginRequest wrongPasswordRequest = new LoginRequest("testuser@example.com", "WrongPassword!");

                for (int i = 0; i < 5; i++) {
                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                                        .andExpect(status().isUnauthorized());
                }

                // Verify account is locked
                User lockedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(lockedUser.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);

                // Attempt login with correct password (should still be rejected)
                LoginRequest correctPasswordRequest = new LoginRequest("testuser@example.com", "CorrectPassword123!");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(correctPasswordRequest)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        }

        @Test
        void testSuccessfulLoginResetsFailedAttempts() throws Exception {
                // Fail 3 times
                LoginRequest wrongPasswordRequest = new LoginRequest("testuser@example.com", "WrongPassword!");

                for (int i = 0; i < 3; i++) {
                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                                        .andExpect(status().isUnauthorized());
                }

                // Verify failed attempts counter
                User userAfterFailures = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(userAfterFailures.getFailedLoginAttempts()).isEqualTo(3);

                // Successful login
                LoginRequest correctPasswordRequest = new LoginRequest("testuser@example.com", "CorrectPassword123!");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(correctPasswordRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists());

                // Verify failed attempts counter was reset
                User userAfterSuccess = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(userAfterSuccess.getFailedLoginAttempts()).isEqualTo(0);
                assertThat(userAfterSuccess.getLockoutExpiration()).isNull();
        }

        @Test
        void testAutoUnlockAfterExpiration() throws Exception {
                // Lock the account
                LoginRequest wrongPasswordRequest = new LoginRequest("testuser@example.com", "WrongPassword!");

                for (int i = 0; i < 5; i++) {
                        mockMvc.perform(post("/api/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                                        .andExpect(status().isUnauthorized());
                }

                // Verify account is locked
                User lockedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(lockedUser.getAccountStatus()).isEqualTo(AccountStatus.LOCKED);

                // Manually set lockout expiration to past (simulating time passing)
                lockedUser.setLockoutExpiration(LocalDateTime.now().minusMinutes(1));
                userRepository.save(lockedUser);

                // Attempt login with correct password (should auto-unlock and succeed)
                LoginRequest correctPasswordRequest = new LoginRequest("testuser@example.com", "CorrectPassword123!");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(correctPasswordRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists());

                // Verify account was unlocked
                User unlockedUser = userRepository.findById(testUser.getId()).orElseThrow();
                assertThat(unlockedUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
                assertThat(unlockedUser.getFailedLoginAttempts()).isEqualTo(0);
                assertThat(unlockedUser.getLockoutExpiration()).isNull();

                // Verify unlock audit record
                List<UserAudit> audits = userAuditRepository.findByUserIdOrderByTimestampDesc(
                                testUser.getId(), org.springframework.data.domain.PageRequest.of(0, 20))
                                .getContent();

                UserAudit unlockAudit = audits.stream()
                                .filter(audit -> audit.getAction() == AuditAction.ACCOUNT_UNLOCKED)
                                .findFirst()
                                .orElseThrow();

                assertThat(unlockAudit.getUserId()).isEqualTo(testUser.getId());
        }

        @Test
        void testFailedLoginAuditRecordsIncludeIpAddress() throws Exception {
                LoginRequest wrongPasswordRequest = new LoginRequest("testuser@example.com", "WrongPassword!");

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(wrongPasswordRequest))
                                .header("X-Forwarded-For", "203.0.113.42"))
                                .andExpect(status().isUnauthorized());

                // Verify audit record includes IP address
                List<UserAudit> audits = userAuditRepository.findByUserIdOrderByTimestampDesc(
                                testUser.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                                .getContent();

                UserAudit failedLoginAudit = audits.stream()
                                .filter(audit -> audit.getAction() == AuditAction.LOGIN_FAILED)
                                .findFirst()
                                .orElseThrow();

                assertThat(failedLoginAudit.getIpAddress()).isEqualTo("203.0.113.42");
        }
}
