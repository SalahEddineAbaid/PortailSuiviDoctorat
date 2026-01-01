package ma.emsi.userservice.controller;

import jakarta.validation.Valid;
import ma.emsi.userservice.dto.request.LoginRequest;
import ma.emsi.userservice.dto.request.RegisterRequest;
import ma.emsi.userservice.dto.response.TokenResponse;
import ma.emsi.userservice.dto.response.UserResponse;
import ma.emsi.userservice.entity.User;
import ma.emsi.userservice.exception.UserAlreadyExistsException;
import ma.emsi.userservice.service.AuthService;
import ma.emsi.userservice.util.IpAddressExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import ma.emsi.userservice.dto.request.TokenRefreshRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final IpAddressExtractor ipAddressExtractor;

    public AuthController(AuthService authService, IpAddressExtractor ipAddressExtractor) {
        this.authService = authService;
        this.ipAddressExtractor = ipAddressExtractor;
    }

    // ✅ Inscription
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User created = authService.register(request);
            UserResponse response = authService.toUserResponse(created);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserAlreadyExistsException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de l'inscription"));
        }
    }

    // ✅ Connexion
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String ipAddress = ipAddressExtractor.extractIpAddress(httpRequest);
            TokenResponse response = authService.login(request, ipAddress);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Identifiants invalides"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la connexion"));
        }
    }

    // ✅ Refresh Token
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        try {
            TokenResponse response = authService.refreshToken(request.refreshToken());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Refresh token invalide ou expiré"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors du rafraîchissement du token"));
        }
    }

}
