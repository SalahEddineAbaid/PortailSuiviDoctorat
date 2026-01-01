package com.devbuild.eurekaserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Eureka Server
 * Enables Basic Authentication for dashboard and API endpoints
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for REST API operations (Eureka is a REST API)
                .csrf(csrf -> csrf.disable())

                // Configure authorization
                .authorizeHttpRequests(auth -> auth
                        // Allow access to Eureka CSS/JS/images without authentication
                        .requestMatchers("/eureka/css/**", "/eureka/js/**", "/eureka/fonts/**").permitAll()

                        // Require authentication for all other endpoints
                        .anyRequest().authenticated())

                // Enable HTTP Basic Authentication
                .httpBasic(basic -> {
                });

        return http.build();
    }
}
