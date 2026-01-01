package ma.emsi.batchservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration for Spring Boot Actuator health checks and monitoring.
 * Provides custom health indicators for all DataSources used by the batch
 * service.
 */
@Configuration
public class ActuatorConfig {

    /**
     * Health indicator for the primary batch database.
     */
    @Bean
    public HealthIndicator batchDbHealthIndicator(@Qualifier("batchDataSource") DataSource dataSource) {
        return () -> checkDataSourceHealth(dataSource, "batchdb");
    }

    /**
     * Health indicator for the user database.
     */
    @Bean
    public HealthIndicator userDbHealthIndicator(@Qualifier("userDataSource") DataSource dataSource) {
        return () -> checkDataSourceHealth(dataSource, "userdb");
    }

    /**
     * Health indicator for the inscription database.
     */
    @Bean
    public HealthIndicator inscriptionDbHealthIndicator(@Qualifier("inscriptionDataSource") DataSource dataSource) {
        return () -> checkDataSourceHealth(dataSource, "inscriptiondb");
    }

    /**
     * Health indicator for the defense database.
     */
    @Bean
    public HealthIndicator defenseDbHealthIndicator(@Qualifier("defenseDataSource") DataSource dataSource) {
        return () -> checkDataSourceHealth(dataSource, "defensedb");
    }

    /**
     * Health indicator for the notification database.
     */
    @Bean
    public HealthIndicator notificationDbHealthIndicator(@Qualifier("notificationDataSource") DataSource dataSource) {
        return () -> checkDataSourceHealth(dataSource, "notificationdb");
    }

    /**
     * Check the health of a DataSource by attempting to get a connection.
     *
     * @param dataSource DataSource to check
     * @param dbName     Name of the database for logging
     * @return Health status
     */
    private Health checkDataSourceHealth(DataSource dataSource, String dbName) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                        .withDetail("database", dbName)
                        .withDetail("status", "Connection successful")
                        .build();
            } else {
                return Health.down()
                        .withDetail("database", dbName)
                        .withDetail("status", "Connection invalid")
                        .build();
            }
        } catch (SQLException e) {
            return Health.down()
                    .withDetail("database", dbName)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
