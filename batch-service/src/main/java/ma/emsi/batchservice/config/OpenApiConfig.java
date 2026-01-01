package ma.emsi.batchservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI configuration for batch-service REST API documentation.
 * Configures Swagger UI with JWT authentication and comprehensive API
 * information.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8085}")
    private String serverPort;

    @Bean
    public OpenAPI batchServiceOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.emsi.ma")
                                .description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Batch Service API")
                .description("""
                        REST API for the Batch Service - a Spring Batch microservice that automates
                        periodic maintenance tasks and batch processing operations for the doctoral
                        management portal.

                        ## Features
                        - Manual job execution and monitoring
                        - Job execution history and statistics
                        - Monthly report generation and download
                        - Real-time job status tracking

                        ## Authentication
                        All endpoints require JWT authentication with ROLE_ADMIN authorization.
                        Include the JWT token in the Authorization header:
                        ```
                        Authorization: Bearer <your_jwt_token>
                        ```

                        ## Rate Limiting
                        Job trigger endpoints are rate-limited to 10 requests per minute per user.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("EMSI Development Team")
                        .email("support@emsi.ma")
                        .url("https://www.emsi.ma"))
                .license(new License()
                        .name("Copyright © 2024 EMSI")
                        .url("https://www.emsi.ma"));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("Enter JWT token obtained from authentication service");
    }
}
