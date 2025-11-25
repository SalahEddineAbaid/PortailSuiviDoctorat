package ma.emsi.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.userservice.dto.request.ChangePasswordRequest;
import ma.emsi.userservice.dto.request.ForgotPasswordRequest;
import ma.emsi.userservice.dto.request.ResetPasswordRequest;
import ma.emsi.userservice.dto.response.UserResponse;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.service.AuthService;
import ma.emsi.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * 🔹 Récupérer un utilisateur par ID (pour communication inter-services)
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(authService.toUserResponse(user));
    }

    /**
     * 🔹 Récupérer le profil de l'utilisateur connecté
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(authService.toUserResponse(user));
    }

    /**
     * 🔹 Mettre à jour le profil de l'utilisateur connecté
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody User updatedUser) {

        updatedUser.setEmail(userDetails.getUsername());
        User user = userService.updateUser(updatedUser);
        return ResponseEntity.ok(authService.toUserResponse(user));
    }

    /**
     * 🔹 Changer le mot de passe
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        userService.changePassword(email, request);

        return ResponseEntity.ok(Map.of(
                "message", "Mot de passe modifié avec succès"));
    }

    /**
     * 🔹 Déconnexion (supprime le refresh token)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal UserDetails userDetails) {
        userService.logout(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
    }

    /**
     * 🔹 [ADMIN] Récupérer tous les utilisateurs
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> responses = users.stream()
                .map(authService::toUserResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * 🔹 Demander la réinitialisation du mot de passe (mot de passe oublié)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.requestPasswordReset(request);

        return ResponseEntity.ok(Map.of(
                "message", "Si l'email existe, un lien de réinitialisation a été envoyé"));
    }

    /**
     * 🔹 Réinitialiser le mot de passe avec le token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPasswordWithToken(request);

        return ResponseEntity.ok(Map.of(
                "message", "Mot de passe réinitialisé avec succès"));
    }

    /**
     * 🔹 [ADMIN] Supprimer un utilisateur
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès"));
    }
}
