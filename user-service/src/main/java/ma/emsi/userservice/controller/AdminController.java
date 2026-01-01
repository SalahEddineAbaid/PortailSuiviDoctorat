package ma.emsi.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.userservice.dto.request.DisableAccountRequest;
import ma.emsi.userservice.dto.response.ConnectionStatisticsResponse;
import ma.emsi.userservice.dto.response.UserResponse;
import ma.emsi.userservice.dto.response.UserStatisticsResponse;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.RoleName;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.repository.RoleRepository;
import ma.emsi.userservice.service.StatisticsService;
import ma.emsi.userservice.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for admin-only operations
 * Requirements: 7.10, 10.4
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final StatisticsService statisticsService;
    private final RoleRepository roleRepository;

    /**
     * Disable a user account
     * POST /api/admin/users/{userId}/disable
     * Requirements: 7.5
     */
    @PostMapping("/users/{userId}/disable")
    public ResponseEntity<Void> disableUser(
            @PathVariable Long userId,
            @Valid @RequestBody DisableAccountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Extract admin ID from authentication
        User admin = userService.findByEmail(userDetails.getUsername());

        // Call userService.disableAccount
        userService.disableAccount(userId, request.reason(), admin.getId());

        // Return 204 No Content
        return ResponseEntity.noContent().build();
    }

    /**
     * Enable a user account
     * POST /api/admin/users/{userId}/enable
     * Requirements: 7.6
     */
    @PostMapping("/users/{userId}/enable")
    public ResponseEntity<Void> enableUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Extract admin ID from authentication
        User admin = userService.findByEmail(userDetails.getUsername());

        // Call userService.enableAccount
        userService.enableAccount(userId, admin.getId());

        // Return 204 No Content
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all disabled user accounts
     * GET /api/admin/users/disabled
     * Requirements: 7.7
     */
    @GetMapping("/users/disabled")
    public ResponseEntity<Page<UserResponse>> getDisabledUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Create pageable
        Pageable pageable = PageRequest.of(page, size);

        // Call userService.getDisabledAccounts
        Page<UserResponse> disabledUsers = userService.getDisabledAccounts(pageable);

        // Return 200 OK with paginated results
        return ResponseEntity.ok(disabledUsers);
    }

    /**
     * Get user statistics
     * GET /api/admin/statistics/users
     * Requirements: 7.8
     */
    @GetMapping("/statistics/users")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics() {

        // Call statisticsService.getUserStatistics
        UserStatisticsResponse statistics = statisticsService.getUserStatistics();

        // Return 200 OK with statistics
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get connection statistics
     * GET /api/admin/statistics/connections
     * Requirements: 7.9
     */
    @GetMapping("/statistics/connections")
    public ResponseEntity<ConnectionStatisticsResponse> getConnectionStatistics() {

        // Call statisticsService.getConnectionStatistics
        ConnectionStatisticsResponse statistics = statisticsService.getConnectionStatistics();

        // Return 200 OK with connection stats
        return ResponseEntity.ok(statistics);
    }

    /**
     * Update user roles (promote/demote)
     * PUT /api/admin/users/{userId}/roles
     * 
     * Permet à l'admin de :
     * - Promouvoir un Doctorant en Directeur
     * - Ajouter/Retirer des rôles
     */
    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<UserResponse> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody Map<String, List<String>> request) {

        List<String> roleNames = request.get("roles");
        if (roleNames == null || roleNames.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        User user = userService.findById(userId);

        // Récupérer les rôles depuis la base
        Set<Role> newRoles = new HashSet<>();
        for (String roleName : roleNames) {
            String normalizedRoleName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            try {
                RoleName roleNameEnum = RoleName.valueOf(normalizedRoleName);
                roleRepository.findByName(roleNameEnum)
                        .ifPresent(newRoles::add);
            } catch (IllegalArgumentException e) {
                // Role name not found in enum, skip
            }
        }

        if (newRoles.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        user.setRoles(newRoles);
        User updatedUser = userService.save(user);

        return ResponseEntity.ok(userService.toUserResponse(updatedUser));
    }

    /**
     * Get all available roles
     * GET /api/admin/roles
     */
    @GetMapping("/roles")
    public ResponseEntity<List<String>> getAllRoles() {
        List<String> roles = roleRepository.findAll().stream()
                .map(role -> role.getName().name())
                .toList();
        return ResponseEntity.ok(roles);
    }

    /**
     * Create a new user with specific role (for creating Directeur accounts)
     * POST /api/admin/users
     */
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        String password = (String) request.get("password");
        String firstName = (String) request.get("firstName");
        String lastName = (String) request.get("lastName");
        String phoneNumber = (String) request.get("phoneNumber");
        String role = (String) request.get("role");

        if (email == null || password == null || firstName == null || lastName == null || role == null) {
            return ResponseEntity.badRequest().build();
        }

        // Vérifier si l'email existe déjà
        if (userService.findByEmail(email) != null) {
            return ResponseEntity.badRequest().build();
        }

        User newUser = userService.createUserWithRole(email, password, firstName, lastName,
                phoneNumber != null ? phoneNumber : "", role);

        return ResponseEntity.ok(userService.toUserResponse(newUser));
    }
}
