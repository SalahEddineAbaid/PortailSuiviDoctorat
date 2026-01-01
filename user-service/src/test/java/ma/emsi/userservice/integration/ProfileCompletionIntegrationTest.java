package ma.emsi.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.emsi.userservice.dto.request.LoginRequest;
import ma.emsi.userservice.dto.request.ProfileCompleteRequest;
import ma.emsi.userservice.dto.request.RegisterRequest;
import ma.emsi.userservice.dto.response.LoginResponse;
import ma.emsi.userservice.dto.response.UserDetailedResponse;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.RoleName;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.entity.UserAudit;
import ma.emsi.userservice.entity.UserProfile;
import ma.emsi.userservice.enums.AuditAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for profile completion flow:
 * complete profile → get detailed profile
 * Verifies profile data persisted and audit record created.
 * 
 * Validates: Requirements 1.1, 1.2, 1.3, 2.5
 */
class ProfileCompletionIntegrationTest extends IntegrationTestBase {

        @Autowired
        private ObjectMapper objectMapper;

        private String accessToken;
        private User testUser;

        @BeforeEach
        void setupUserAndLogin() throws Exception {
                // Ensure roles exist
                if (roleRepository.findByName(RoleName.ROLE_DOCTORANT).isEmpty()) {
                        Role doctorantRole = new Role();
                        doctorantRole.setName(RoleName.ROLE_DOCTORANT);
                        roleRepository.save(doctorantRole);
                }

                // Register a user
                RegisterRequest registerRequest = new RegisterRequest(
                                "doctorant@example.com",
                                "Password123!",
                                "Ahmed",
                                "Benali",
                                "0612345680",
                                "789 University Rd",
                                "Fes",
                                "Morocco",
                                null);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isCreated());

                testUser = userRepository.findByEmail("doctorant@example.com").orElseThrow();

                // Login to get access token
                LoginRequest loginRequest = new LoginRequest("doctorant@example.com", "Password123!");

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andReturn();

                String loginResponseJson = loginResult.getResponse().getContentAsString();
                LoginResponse loginResponse = objectMapper.readValue(loginResponseJson, LoginResponse.class);
                accessToken = loginResponse.accessToken();
        }

        @Test
        void testCompleteProfileFlow() throws Exception {
                // Step 1: Complete profile
                ProfileCompleteRequest profileRequest = new ProfileCompleteRequest(
                                LocalDate.of(1995, 5, 15),
                                "Casablanca",
                                "Moroccan",
                                "AB123456",
                                "https://example.com/photo.jpg");

                MvcResult completeResult = mockMvc.perform(put("/api/users/profile/complete")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(profileRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.profileComplete").value(true))
                                .andExpect(jsonPath("$.profile.cin").value("AB123456"))
                                .andExpect(jsonPath("$.profile.nationalite").value("Moroccan"))
                                .andExpect(jsonPath("$.profile.lieuNaissance").value("Casablanca"))
                                .andReturn();

                String responseJson = completeResult.getResponse().getContentAsString();
                UserDetailedResponse response = objectMapper.readValue(responseJson, UserDetailedResponse.class);

                assertThat(response.profileComplete()).isTrue();
                assertThat(response.profile()).isNotNull();
                assertThat(response.profile().cin()).isEqualTo("AB123456");
                assertThat(response.profile().dateNaissance()).isEqualTo(LocalDate.of(1995, 5, 15));

                // Step 2: Verify profile data was persisted
                UserProfile savedProfile = userProfileRepository.findByUserId(testUser.getId()).orElseThrow();
                assertThat(savedProfile.getCin()).isEqualTo("AB123456");
                assertThat(savedProfile.getNationalite()).isEqualTo("Moroccan");
                assertThat(savedProfile.getLieuNaissance()).isEqualTo("Casablanca");
                assertThat(savedProfile.getDateNaissance()).isEqualTo(LocalDate.of(1995, 5, 15));
                assertThat(savedProfile.getPhotoUrl()).isEqualTo("https://example.com/photo.jpg");

                // Step 3: Verify audit record was created
                List<UserAudit> audits = userAuditRepository.findByUserIdOrderByTimestampDesc(
                                testUser.getId(), org.springframework.data.domain.PageRequest.of(0, 10))
                                .getContent();

                UserAudit profileAudit = audits.stream()
                                .filter(audit -> audit.getAction() == AuditAction.PROFILE_MODIFIED)
                                .findFirst()
                                .orElseThrow();

                assertThat(profileAudit.getUserId()).isEqualTo(testUser.getId());

                // Step 4: Get detailed profile
                mockMvc.perform(get("/api/users/" + testUser.getId() + "/profile-complete")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.profileComplete").value(true))
                                .andExpect(jsonPath("$.profile.cin").value("AB123456"))
                                .andExpect(jsonPath("$.email").value("doctorant@example.com"));
        }

        @Test
        void testCompleteProfile_DuplicateCin_ReturnsConflict() throws Exception {
                // Create first user with profile
                ProfileCompleteRequest firstProfile = new ProfileCompleteRequest(
                                LocalDate.of(1990, 1, 1),
                                "Rabat",
                                "Moroccan",
                                "CD789012",
                                null);

                mockMvc.perform(put("/api/users/profile/complete")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(firstProfile)))
                                .andExpect(status().isOk());

                // Register second user
                RegisterRequest secondUserRequest = new RegisterRequest(
                                "second@example.com",
                                "Password123!",
                                "Sara",
                                "Alami",
                                "0612345681",
                                "321 Street",
                                "Marrakech",
                                "Morocco",
                                null);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(secondUserRequest)))
                                .andExpect(status().isCreated());

                // Login as second user
                LoginRequest secondLogin = new LoginRequest("second@example.com", "Password123!");
                MvcResult secondLoginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(secondLogin)))
                                .andExpect(status().isOk())
                                .andReturn();

                String secondLoginJson = secondLoginResult.getResponse().getContentAsString();
                LoginResponse secondLoginResponse = objectMapper.readValue(secondLoginJson, LoginResponse.class);
                String secondAccessToken = secondLoginResponse.accessToken();

                // Try to complete profile with duplicate CIN
                ProfileCompleteRequest duplicateProfile = new ProfileCompleteRequest(
                                LocalDate.of(1992, 3, 10),
                                "Tangier",
                                "Moroccan",
                                "CD789012", // Same CIN as first user
                                null);

                mockMvc.perform(put("/api/users/profile/complete")
                                .header("Authorization", "Bearer " + secondAccessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicateProfile)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.code").value("DUPLICATE_CIN"));
        }

        @Test
        void testUpdateProfile_Idempotence() throws Exception {
                // Complete profile first time
                ProfileCompleteRequest firstRequest = new ProfileCompleteRequest(
                                LocalDate.of(1993, 7, 20),
                                "Agadir",
                                "Moroccan",
                                "EF345678",
                                "https://example.com/photo1.jpg");

                mockMvc.perform(put("/api/users/profile/complete")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(firstRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.profileComplete").value(true));

                // Update profile with new data
                ProfileCompleteRequest updateRequest = new ProfileCompleteRequest(
                                LocalDate.of(1993, 7, 20),
                                "Agadir",
                                "Moroccan",
                                "EF345678",
                                "https://example.com/photo2.jpg" // Updated photo URL
                );

                mockMvc.perform(put("/api/users/profile/complete")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.profileComplete").value(true))
                                .andExpect(jsonPath("$.profile.photoUrl").value("https://example.com/photo2.jpg"));

                // Verify only one profile exists
                UserProfile profile = userProfileRepository.findByUserId(testUser.getId()).orElseThrow();
                assertThat(profile.getPhotoUrl()).isEqualTo("https://example.com/photo2.jpg");

                // Verify there's still only one profile record
                long profileCount = userProfileRepository.count();
                assertThat(profileCount).isEqualTo(1);
        }
}
