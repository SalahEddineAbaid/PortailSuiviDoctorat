package ma.emsi.userservice.service;

import lombok.RequiredArgsConstructor;
import ma.emsi.userservice.entity.RefreshToken;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.exception.RefreshTokenException;
import ma.emsi.userservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // durée de validité configurable (en millisecondes)
    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenDurationMs;

    /**
     * 🔹 Crée un refresh token pour l’utilisateur
     */
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * 🔹 Recherche un refresh token par sa valeur
     */
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RefreshTokenException("Refresh token invalide"));
    }

    /**
     * 🔹 Vérifie s’il n’est pas expiré
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenException("Refresh token expiré. Veuillez vous reconnecter.");
        }
        return token;
    }

    /**
     * 🔹 Supprime tous les tokens liés à un utilisateur (pour le logout)
     */
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
