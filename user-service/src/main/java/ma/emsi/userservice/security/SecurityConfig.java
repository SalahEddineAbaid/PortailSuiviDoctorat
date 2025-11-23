package ma.emsi.userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ✅ CORS doit être en PREMIER
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ✅ Désactiver CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // ✅ Configuration des autorisations
                .authorizeHttpRequests(auth -> auth
                        // Routes publiques (authentification)
                        .requestMatchers("/api/auth/**").permitAll()

                        // Routes publiques (mot de passe oublié) - DOIT ÊTRE AVANT /api/users/**
                        .requestMatchers("/api/users/forgot-password", "/api/users/reset-password").permitAll()

                        // Routes techniques publiques
                        .requestMatchers("/error", "/actuator/**").permitAll()

                        // Routes protégées par rôle
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRATEUR")
                        .requestMatchers("/api/directeur/**").hasRole("DIRECTEUR")
                        .requestMatchers("/api/doctorant/**").hasRole("DOCTORANT")
                        .requestMatchers("/api/users/**").authenticated()

                        // Toutes les autres requêtes nécessitent une authentification
                        .anyRequest().authenticated()
                )

                // ✅ Session stateless (JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ✅ Ajouter le filtre JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Autoriser l'origine Angular
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));

        // ✅ Autoriser toutes les méthodes HTTP
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // ✅ Autoriser tous les headers
        configuration.setAllowedHeaders(List.of("*"));

        // ✅ Permettre les credentials (cookies, authorization)
        configuration.setAllowCredentials(true);

        // ✅ Exposer les headers de réponse
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // ✅ Durée de cache de la requête preflight
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}