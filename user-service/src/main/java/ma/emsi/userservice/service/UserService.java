package ma.emsi.userservice.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.userservice.dto.request.ChangePasswordRequest;
import ma.emsi.userservice.dto.request.ForgotPasswordRequest;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.RoleName;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.repository.UserRepository;
import ma.emsi.userservice.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import ma.emsi.userservice.dto.request.ResetPasswordRequest;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordRequestService passwordRequestService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ma.emsi.userservice.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur avec l'ID " + id + " non trouvé"));
    }

    public User assignRole(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable"));

        user.getRoles().add(role);
        return userRepository.save(user);
    }

    public User removeRole(Long userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        user.getRoles().removeIf(role -> role.getName().equals(roleName));
        return userRepository.save(user);
    }

    public User updateUser(User updatedUser) {
        User existing = userRepository.findByEmail(updatedUser.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        existing.setFirstName(updatedUser.getFirstName());
        existing.setLastName(updatedUser.getLastName());
        existing.setAdresse(updatedUser.getAdresse());
        existing.setVille(updatedUser.getVille());
        existing.setPays(updatedUser.getPays());
        existing.setPhoneNumber(updatedUser.getPhoneNumber());

        return userRepository.save(existing);
    }

    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        refreshTokenService.deleteByUser(user);
    }

    public void changePassword(String email, @Valid ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        // Mettre à jour le mot de passe (validation déjà faite par @Valid)
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    public void requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordRequestService.requestPasswordReset(request);
    }

    public void resetPasswordWithToken(ResetPasswordRequest request) {
        passwordRequestService.resetPassword(request);
    }

    /**
     * Réinitialise le mot de passe sans vérifier l'ancien
     * (utilisé lors de la réinitialisation par token)
     */
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Valider le nouveau mot de passe
        if (newPassword == null || newPassword.length() < 12) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit contenir au moins 6 caractères");
        }

        // Encoder et sauvegarder le nouveau mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Supprimer toutes les dépendances avant de supprimer l'utilisateur
        refreshTokenService.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);

        // Ensuite supprimer l'utilisateur
        userRepository.deleteById(id);
    }
}