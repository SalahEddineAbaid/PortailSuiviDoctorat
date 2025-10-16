package ma.emsi.userservice.service;


import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ma.emsi.userservice.dto.request.RegisterRequest;
import ma.emsi.userservice.dto.request.LoginRequest;
import ma.emsi.userservice.dto.response.TokenResponse;
import ma.emsi.userservice.dto.response.UserResponse;
import ma.emsi.userservice.entity.RefreshToken;
import ma.emsi.userservice.entity.Role;
import ma.emsi.userservice.entity.RoleName;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.repository.RefreshTokenRepository;
import ma.emsi.userservice.repository.RoleRepository;
import ma.emsi.userservice.repository.UserRepository;
import ma.emsi.userservice.security.JwtProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public User register(RegisterRequest request) {
        logger.info("Tentative d'inscription pour l'email : {}", request.email());
        if (userRepository.existsByEmail(request.email())) {
            logger.warn("Échec de l'inscription : email déjà utilisé ({})", request.email());
            throw new RuntimeException("Cet Email est déjà utilisé, Veuillez en choisir un autre.");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());
        user.setAdresse(request.adresse());
        user.setVille(request.ville());
        user.setPays(request.pays());
        user.setEnabled(true);

        Role roleDoctorant = roleRepository.findByName(RoleName.ROLE_DOCTORANT)
                .orElseThrow(() -> new RuntimeException("Le rôle ROLE_DOCTORANT n'existe pas"));
        user.getRoles().add(roleDoctorant);
        logger.info("Utilisateur créé avec succès : {}", user.getId());
        return userRepository.save(user);
    }

    public UserResponse toUserResponse(User u) {
        var roles = u.getRoles()
                .stream()
                .map(r -> r.getName().name())
                .collect(java.util.stream.Collectors.toSet());

        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getPhoneNumber(),
                u.getAdresse(),
                u.getVille(),
                u.getPays(),
                roles
        );
    }

    public TokenResponse login(LoginRequest request) {
        try {
            // Authentification de l'utilisateur
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(),
                            request.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Génération des tokens
            String accessToken = jwtProvider.generateToken(authentication);
            String refreshToken = UUID.randomUUID().toString();

            // Récupération de l'utilisateur
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

            // Enregistrement du refresh token
            RefreshToken token = RefreshToken.builder()
                    .token(refreshToken)
                    .user(user)
                    .expiryDate(Instant.now().plus(7, ChronoUnit.DAYS))
                    .build();
            refreshTokenRepository.save(token);

            return new TokenResponse(accessToken, refreshToken);
        } catch (AuthenticationException ex) {
            throw ex;
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        // Validation du refresh token
        if (refreshToken == null || !refreshTokenRepository.existsByToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token invalide ou expiré");
        }

        // Récupération du token
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token introuvable"));

        // Vérification de l'expiration
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token expiré");
        }

        // Génération d'un nouveau access token
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                token.getUser().getEmail(),
                null,
                token.getUser().getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                        .collect(Collectors.toList())
        );
        String newAccessToken = jwtProvider.generateToken(authentication);

        return new TokenResponse(newAccessToken, refreshToken);
    }

}
