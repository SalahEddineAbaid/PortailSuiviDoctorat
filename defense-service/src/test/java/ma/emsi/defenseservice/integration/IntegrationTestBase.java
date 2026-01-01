package ma.emsi.defenseservice.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Base class for integration tests with Testcontainers, WireMock, and embedded
 * Kafka
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = { "defense-events" }, brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
})
public abstract class IntegrationTestBase {

    @Container
    protected static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:10.11")
            .withDatabaseName("defense_service_test")
            .withUsername("test")
            .withPassword("test");

    protected static WireMockServer wireMockServer;

    @Autowired
    protected DataSource dataSource;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MariaDB configuration
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");

        // User Service configuration (WireMock)
        registry.add("user-service.url", () -> "http://localhost:8089");

        // Disable Eureka for tests
        registry.add("eureka.client.enabled", () -> "false");
    }

    @BeforeAll
    static void setupWireMock() {
        wireMockServer = new WireMockServer(options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void tearDownWireMock() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @BeforeEach
    void cleanDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            // Disable foreign key checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            // Clean all tables
            stmt.execute("TRUNCATE TABLE autorisations_soutenance");
            stmt.execute("TRUNCATE TABLE defenses");
            stmt.execute("TRUNCATE TABLE rapports");
            stmt.execute("TRUNCATE TABLE jury_members");
            stmt.execute("TRUNCATE TABLE juries");
            stmt.execute("TRUNCATE TABLE documents");
            stmt.execute("TRUNCATE TABLE publications");
            stmt.execute("TRUNCATE TABLE defense_requests");
            stmt.execute("TRUNCATE TABLE prerequisites");

            // Re-enable foreign key checks
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    /**
     * Setup WireMock stub for user validation
     */
    protected void stubUserValidation(Long userId, String role, boolean isValid) {
        stubFor(get(urlEqualTo("/api/users/" + userId + "/validate-role?role=" + role))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.valueOf(isValid))));
    }

    /**
     * Setup WireMock stub for user details
     */
    protected void stubUserDetails(Long userId, String name, String email) {
        stubFor(get(urlEqualTo("/api/users/" + userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format(
                                "{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\"}",
                                userId, name, email))));
    }

    /**
     * Setup WireMock stub for user service failure
     */
    protected void stubUserServiceFailure(Long userId) {
        stubFor(get(urlMatching("/api/users/" + userId + ".*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));
    }
}
