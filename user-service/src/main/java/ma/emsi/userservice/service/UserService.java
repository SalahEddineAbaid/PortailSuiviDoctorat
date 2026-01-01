package ma.emsi.userservice.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.userservice.dto.request.ChangePasswordRequest;
import ma.emsi.userservice.dto.request.ForgotPasswordRequest;
import ma.emsi.userservice.dto.request.ProfileCompleteRequest;
import ma.emsi.userservice.dto.response.ProfileData;
import ma.emsi.userservice.dto.response.UserDetailedResponse;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.RoleName;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.entity.UserProfile;
import ma.emsi.userservice.exception.DuplicateCinException;
import ma.emsi.userservice.repository.UserRepository;
import ma.emsi.userservice.repository.RoleRepository;
import ma.emsi.userservice.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import ma.emsi.userservice.dto.request.ResetPasswordRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordRequestService passwordRequestService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ma.emsi.userservice.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuditService auditService;
    private final UserEventPublisher eventPublisher;

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

    @Transactional
    public UserDetailedResponse completeProfile(Long userId, ProfileCompleteRequest request) {
        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Check CIN uniqueness (excluding current user's profile if updating)
        UserProfile existingProfile = userProfileRepository.findByUserId(userId).orElse(null);
        if (existingProfile == null || !request.cin().equals(existingProfile.getCin())) {
            if (userProfileRepository.existsByCin(request.cin())) {
                throw new DuplicateCinException("Ce CIN est déjà utilisé par un autre utilisateur");
            }
        }

        // Create or update UserProfile
        UserProfile profile;
        if (existingProfile != null) {
            profile = existingProfile;
        } else {
            profile = new UserProfile();
            profile.setUser(user);
        }

        profile.setDateNaissance(request.dateNaissance());
        profile.setLieuNaissance(request.lieuNaissance());
        profile.setNationalite(request.nationalite());
        profile.setCin(request.cin());
        profile.setPhotoUrl(request.photoUrl());

        userProfileRepository.save(profile);

        // Set profileComplete flag
        user.setProfileComplete(true);
        user.setProfile(profile);
        userRepository.save(user);

        // Log profile modified audit
        auditService.logProfileModified(userId, null);

        // Publish event
        eventPublisher.publishProfileCompleted(userId);

        // Return detailed response
        return buildUserDetailedResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDetailedResponse getDetailedProfile(Long userId) {
        // Fetch user with profile
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Return detailed response
        return buildUserDetailedResponse(user);
    }

    private UserDetailedResponse buildUserDetailedResponse(User user) {
        ProfileData profileData = null;
        if (user.getProfile() != null) {
            UserProfile profile = user.getProfile();
            profileData = new ProfileData(
                    profile.getDateNaissance(),
                    profile.getLieuNaissance(),
                    profile.getNationalite(),
                    profile.getCin(),
                    profile.getPhotoUrl());
        }

        return new UserDetailedResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAdresse(),
                user.getVille(),
                user.getPays(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()),
                user.isProfileComplete(),
                profileData,
                user.getAccountStatus());
    }

    @Transactional
    public void disableAccount(Long userId, String reason, Long adminId) {
        // Validate admin cannot disable self
        if (userId.equals(adminId)) {
            throw new ma.emsi.userservice.exception.SelfDisableException(
                    "Un administrateur ne peut pas désactiver son propre compte");
        }

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Set account status to DISABLED
        user.setAccountStatus(ma.emsi.userservice.enums.AccountStatus.DISABLED);
        userRepository.save(user);

        // Invalidate all refresh tokens
        refreshTokenService.deleteByUser(user);

        // Log account disabled audit
        auditService.logAccountDisabled(userId, reason, adminId);

        // Publish USER_DISABLED event
        eventPublisher.publishUserDisabled(userId, user.getEmail(), reason);
    }

    @Transactional
    public void enableAccount(Long userId, Long adminId) {
        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Set account status to ACTIVE
        user.setAccountStatus(ma.emsi.userservice.enums.AccountStatus.ACTIVE);

        // Reset failed login attempts
        user.setFailedLoginAttempts(0);
        user.setLockoutExpiration(null);

        userRepository.save(user);

        // Log account enabled audit
        auditService.logAccountEnabled(userId, adminId);

        // Publish USER_ENABLED event
        eventPublisher.publishUserEnabled(userId, user.getEmail());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ma.emsi.userservice.dto.response.UserResponse> getDisabledAccounts(
            org.springframework.data.domain.Pageable pageable) {
        // Query disabled accounts with pagination
        org.springframework.data.domain.Page<User> disabledUsers = userRepository.findByAccountStatus(
                ma.emsi.userservice.enums.AccountStatus.DISABLED, pageable);

        // Map to response DTOs
        return disabledUsers.map(user -> new ma.emsi.userservice.dto.response.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAdresse(),
                user.getVille(),
                user.getPays(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet())));
    }

    /**
     * Sauvegarder un utilisateur
     */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * Convertir un User en UserResponse
     */
    public ma.emsi.userservice.dto.response.UserResponse toUserResponse(User user) {
        return new ma.emsi.userservice.dto.response.UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAdresse(),
                user.getVille(),
                user.getPays(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()));
    }

    /**
     * Créer un utilisateur avec un rôle spécifique
     * Utilisé par l'admin pour créer des comptes Directeur
     */
    @Transactional
    public User createUserWithRole(String email, String password, String firstName,
            String lastName, String phoneNumber, String roleName) {
        // Normaliser le nom du rôle
        String normalizedRoleName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;

        // Convertir en RoleName enum et récupérer le rôle
        RoleName roleNameEnum;
        try {
            roleNameEnum = RoleName.valueOf(normalizedRoleName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide: " + normalizedRoleName);
        }

        Role role = roleRepository.findByName(roleNameEnum)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable: " + normalizedRoleName));

        // Créer l'utilisateur
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber != null ? phoneNumber : "");
        user.setAdresse("");
        user.setVille("");
        user.setPays("Maroc");
        user.setEnabled(true);
        user.setAccountStatus(ma.emsi.userservice.enums.AccountStatus.ACTIVE);
        user.setRoles(java.util.Set.of(role));

        return userRepository.save(user);
    }
}