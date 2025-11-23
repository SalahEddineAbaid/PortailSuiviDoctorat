package ma.emsi.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.userservice.dto.request.ForgotPasswordRequest;
import ma.emsi.userservice.dto.request.ResetPasswordRequest;
import ma.emsi.userservice.entity.PasswordResetToken;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.repository.PasswordResetTokenRepository;
import ma.emsi.userservice.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordRequestService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 🔹 Génère un token de réinitialisation et envoie un email
     */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            log.warn("Tentative de réinitialisation pour email inexistant : {}", request.email());
            return; // ✅ On ne fait rien mais on ne lève pas d'exception
        }

        User user = userOpt.get();

        // Invalider les anciens tokens
        tokenRepository.findByUserAndUsedFalse(user)
                .forEach(token -> {
                    token.setUsed(true);
                    tokenRepository.save(token);
                });

        // Générer un nouveau token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expirationDate(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        // Envoyer l'email
        emailService.sendPasswordResetEmail(user.getEmail(), token);

        log.info("Token de réinitialisation généré pour l'utilisateur : {}", user.getEmail());
    }


    /**
     * 🔹 Réinitialise le mot de passe avec un token valide
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepository.findByToken(request.token())
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (!resetToken.isValid()) {
            throw new RuntimeException("Le token est expiré ou déjà utilisé");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Mot de passe réinitialisé avec succès pour l'utilisateur : {}", user.getEmail());
    }

    /**
     * 🔹 Nettoie automatiquement les tokens expirés (tous les jours à 2h du matin)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deletedCount = tokenRepository.deleteByExpirationDateBefore(now);
        log.info("Nettoyage automatique : {} tokens expirés supprimés", deletedCount);
    }
}
