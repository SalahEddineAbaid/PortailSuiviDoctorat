package ma.emsi.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.emsi.userservice.dto.request.LoginRequest;
import ma.emsi.userservice.dto.request.RegisterRequest;
import ma.emsi.userservice.dto.response.LoginResponse;
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

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for admin statistics:
 * create test data → query statistics
 * Verifies accuracy of counts.
 * 
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5
 */
class AdminStatisticsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminAccessToken;
    private User adminUser;

    @BeforeEach
    void setupAdminAndLogin() throws Exception {
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

        Role directeurRole = roleRepository.findByName(RoleName.ROLE_DIRECTEUR)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.ROLE_DIRECTEUR);
                    return roleRepository.save(role);
                });

        // Create admin user
        adminUser = new User();
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("AdminPass123!"));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setPhoneNumber("0612345685");
        adminUser.setAdresse("Admin Street");
        adminUser.setVille("Casablanca");
        adminUser.setPays("Morocco");
        adminUser.setRoles(Set.of(adminRole));
        adminUser.setAccountStatus(AccountStatus.ACTIVE);
        adminUser = userRepository.save(adminUser);

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
    void testUserStatistics() throws Exception {
        // Create test users with different roles and statuses
        Role doctorantRole = roleRepository.findByName(RoleName.ROLE_DOCTORANT).orElseThrow();
        Role directeurRole = roleRepository.findByName(RoleName.ROLE_DIRECTEUR).orElseThrow();

        // Create 3 doctorants (2 active, 1 disabled)
        for (int i = 1; i <= 3; i++) {
            User doctorant = new User();
            doctorant.setEmail("doctorant" + i + "@example.com");
            doctorant.setPassword(passwordEncoder.encode("Password123!"));
            doctorant.setFirstName("Doctorant");
            doctorant.setLastName("User" + i);
            doctorant.setPhoneNumber("061234568" + i);
            doctorant.setAdresse("Address " + i);
            doctorant.setVille("City " + i);
            doctorant.setPays("Morocco");
            doctorant.setRoles(Set.of(doctorantRole));
            doctorant.setAccountStatus(i == 3 ? AccountStatus.DISABLED : AccountStatus.ACTIVE);
            userRepository.save(doctorant);
        }

        // Create 2 directeurs (both active)
        for (int i = 1; i <= 2; i++) {
            User directeur = new User();
            directeur.setEmail("directeur" + i + "@example.com");
            directeur.setPassword(passwordEncoder.encode("Password123!"));
            directeur.setFirstName("Directeur");
            directeur.setLastName("User" + i);
            directeur.setPhoneNumber("061234569" + i);
            directeur.setAdresse("Address " + i);
            directeur.setVille("City " + i);
            directeur.setPays("Morocco");
            directeur.setRoles(Set.of(directeurRole));
            directeur.setAccountStatus(AccountStatus.ACTIVE);
            userRepository.save(directeur);
        }

        // Query user statistics
        MvcResult result = mockMvc.perform(get("/api/admin/statistics/users")
                .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6)) // 1 admin + 3 doctorants + 2 directeurs
                .andExpect(jsonPath("$.active").value(5)) // All except 1 disabled doctorant
                .andExpect(jsonPath("$.disabled").value(1))
                .andExpect(jsonPath("$.locked").value(0))
                .andExpect(jsonPath("$.byRole.ROLE_ADMIN").value(1))
                .andExpect(jsonPath("$.byRole.ROLE_DOCTORANT").value(3))
                .andExpect(jsonPath("$.byRole.ROLE_DIRECTEUR").value(2))
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertThat(responseJson).contains("\"total\":6");
        assertThat(responseJson).contains("\"active\":5");
        assertThat(responseJson).contains("\"disabled\":1");
    }

    @Test
    void testConnectionStatistics() throws Exception {
        // Create test users
        Role doctorantRole = roleRepository.findByName(RoleName.ROLE_DOCTORANT).orElseThrow();
        Role directeurRole = roleRepository.findByName(RoleName.ROLE_DIRECTEUR).orElseThrow();

        User doctorant = new User();
        doctorant.setEmail("doctorant@example.com");
        doctorant.setPassword(passwordEncoder.encode("Password123!"));
        doctorant.setFirstName("Test");
        doctorant.setLastName("Doctorant");
        doctorant.setPhoneNumber("0612345690");
        doctorant.setAdresse("Address");
        doctorant.setVille("City");
        doctorant.setPays("Morocco");
        doctorant.setRoles(Set.of(doctorantRole));
        doctorant.setAccountStatus(AccountStatus.ACTIVE);
        doctorant = userRepository.save(doctorant);

        User directeur = new User();
        directeur.setEmail("directeur@example.com");
        directeur.setPassword(passwordEncoder.encode("Password123!"));
        directeur.setFirstName("Test");
        directeur.setLastName("Directeur");
        directeur.setPhoneNumber("0612345691");
        directeur.setAdresse("Address");
        directeur.setVille("City");
        directeur.setPays("Morocco");
        directeur.setRoles(Set.of(directeurRole));
        directeur.setAccountStatus(AccountStatus.ACTIVE);
        directeur = userRepository.save(directeur);

        // Create login audit records for different dates
        LocalDateTime now = LocalDateTime.now();

        // Doctorant logins (3 today, 2 yesterday)
        for (int i = 0; i < 3; i++) {
            UserAudit audit = new UserAudit();
            audit.setUserId(doctorant.getId());
            audit.setAction(AuditAction.LOGIN);
            audit.setTimestamp(now.minusHours(i));
            audit.setIpAddress("192.168.1." + i);
            userAuditRepository.save(audit);
        }

        for (int i = 0; i < 2; i++) {
            UserAudit audit = new UserAudit();
            audit.setUserId(doctorant.getId());
            audit.setAction(AuditAction.LOGIN);
            audit.setTimestamp(now.minusDays(1).minusHours(i));
            audit.setIpAddress("192.168.1." + (i + 10));
            userAuditRepository.save(audit);
        }

        // Directeur logins (2 today)
        for (int i = 0; i < 2; i++) {
            UserAudit audit = new UserAudit();
            audit.setUserId(directeur.getId());
            audit.setAction(AuditAction.LOGIN);
            audit.setTimestamp(now.minusHours(i));
            audit.setIpAddress("192.168.2." + i);
            userAuditRepository.save(audit);
        }

        // Admin login (1 today)
        UserAudit adminAudit = new UserAudit();
        adminAudit.setUserId(adminUser.getId());
        adminAudit.setAction(AuditAction.LOGIN);
        adminAudit.setTimestamp(now);
        adminAudit.setIpAddress("192.168.3.1");
        userAuditRepository.save(adminAudit);

        // Query connection statistics
        MvcResult result = mockMvc.perform(get("/api/admin/statistics/connections")
                .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyCounts").isArray())
                .andExpect(jsonPath("$.byRole").exists())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();

        // Verify response contains connection data
        assertThat(responseJson).contains("dailyCounts");
        assertThat(responseJson).contains("byRole");

        // The response should include role-based breakdown
        assertThat(responseJson).contains("ROLE_DOCTORANT");
        assertThat(responseJson).contains("ROLE_DIRECTEUR");
        assertThat(responseJson).contains("ROLE_ADMIN");
    }

    @Test
    void testNonAdminCannotAccessStatistics() throws Exception {
        // Register a regular user
        RegisterRequest regularUserRequest = new RegisterRequest(
                "regular@example.com",
                "Password123!",
                "Regular",
                "User",
                "0612345692",
                "Regular Street",
                "Rabat",
                "Morocco",
                null);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regularUserRequest)))
                .andExpect(status().isCreated());

        // Login as regular user
        LoginRequest regularLogin = new LoginRequest("regular@example.com", "Password123!");
        MvcResult regularLoginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regularLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String regularLoginJson = regularLoginResult.getResponse().getContentAsString();
        LoginResponse regularLoginResponse = objectMapper.readValue(regularLoginJson, LoginResponse.class);
        String regularAccessToken = regularLoginResponse.accessToken();

        // Attempt to access user statistics (should be forbidden)
        mockMvc.perform(get("/api/admin/statistics/users")
                .header("Authorization", "Bearer " + regularAccessToken))
                .andExpect(status().isForbidden());

        // Attempt to access connection statistics (should be forbidden)
        mockMvc.perform(get("/api/admin/statistics/connections")
                .header("Authorization", "Bearer " + regularAccessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testNewUsersThisMonthCount() throws Exception {
        // Create users with different creation dates
        Role doctorantRole = roleRepository.findByName(RoleName.ROLE_DOCTORANT).orElseThrow();
        LocalDateTime now = LocalDateTime.now();

        // User created this month
        User thisMonthUser = new User();
        thisMonthUser.setEmail("thismonth@example.com");
        thisMonthUser.setPassword(passwordEncoder.encode("Password123!"));
        thisMonthUser.setFirstName("This");
        thisMonthUser.setLastName("Month");
        thisMonthUser.setPhoneNumber("0612345693");
        thisMonthUser.setAdresse("Address");
        thisMonthUser.setVille("City");
        thisMonthUser.setPays("Morocco");
        thisMonthUser.setRoles(Set.of(doctorantRole));
        thisMonthUser.setAccountStatus(AccountStatus.ACTIVE);
        thisMonthUser.setCreatedAt(now.minusDays(5));
        userRepository.save(thisMonthUser);

        // User created last month
        User lastMonthUser = new User();
        lastMonthUser.setEmail("lastmonth@example.com");
        lastMonthUser.setPassword(passwordEncoder.encode("Password123!"));
        lastMonthUser.setFirstName("Last");
        lastMonthUser.setLastName("Month");
        lastMonthUser.setPhoneNumber("0612345694");
        lastMonthUser.setAdresse("Address");
        lastMonthUser.setVille("City");
        lastMonthUser.setPays("Morocco");
        lastMonthUser.setRoles(Set.of(doctorantRole));
        lastMonthUser.setAccountStatus(AccountStatus.ACTIVE);
        lastMonthUser.setCreatedAt(now.minusMonths(1).minusDays(5));
        userRepository.save(lastMonthUser);

        // Query statistics
        mockMvc.perform(get("/api/admin/statistics/users")
                .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newThisMonth").exists());
    }
}
