package ma.emsi.batchservice.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Multi-database configuration for batch-service.
 * Configures primary DataSource for batchdb and secondary DataSources for
 * userdb, inscriptiondb, defensedb, and notificationdb.
 * All DataSources use HikariCP connection pooling.
 * Database credentials are externalized to environment variables.
 */
@Configuration
public class MultiDataSourceConfig {

    // ========== Primary DataSource - batchdb ==========

    /**
     * Primary DataSource properties for batchdb.
     * Used by Spring Batch for metadata tables and job execution history.
     */
    @Primary
    @Bean(name = "batchDataSourceProperties")
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties batchDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Primary DataSource for batchdb with HikariCP connection pooling.
     * This is the main database for batch-service operations.
     */
    @Primary
    @Bean(name = "batchDataSource")
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource batchDataSource(
            @Qualifier("batchDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * JdbcTemplate for batchdb operations.
     */
    @Primary
    @Bean(name = "batchJdbcTemplate")
    public JdbcTemplate batchJdbcTemplate(@Qualifier("batchDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ========== Secondary DataSource - userdb ==========

    /**
     * Secondary DataSource properties for userdb.
     * Used for token cleanup operations (read and delete permissions).
     */
    @Bean(name = "userDataSourceProperties")
    @ConfigurationProperties("datasource.userdb")
    public DataSourceProperties userDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Secondary DataSource for userdb with HikariCP connection pooling.
     * Used to access user-service database for token cleanup.
     */
    @Bean(name = "userDataSource")
    @ConfigurationProperties("datasource.userdb.hikari")
    public DataSource userDataSource(
            @Qualifier("userDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * JdbcTemplate for userdb operations.
     */
    @Bean(name = "userJdbcTemplate")
    public JdbcTemplate userJdbcTemplate(@Qualifier("userDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ========== Secondary DataSource - inscriptiondb ==========

    /**
     * Secondary DataSource properties for inscriptiondb.
     * Used for duration alerts and archiving (read-only permissions).
     */
    @Bean(name = "inscriptionDataSourceProperties")
    @ConfigurationProperties("datasource.inscriptiondb")
    public DataSourceProperties inscriptionDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Secondary DataSource for inscriptiondb with HikariCP connection pooling.
     * Used to access inscription-service database for enrollment data.
     */
    @Bean(name = "inscriptionDataSource")
    @ConfigurationProperties("datasource.inscriptiondb.hikari")
    public DataSource inscriptionDataSource(
            @Qualifier("inscriptionDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * JdbcTemplate for inscriptiondb operations.
     */
    @Bean(name = "inscriptionJdbcTemplate")
    public JdbcTemplate inscriptionJdbcTemplate(
            @Qualifier("inscriptionDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ========== Secondary DataSource - defensedb ==========

    /**
     * Secondary DataSource properties for defensedb.
     * Used for defense statistics and archiving (read-only permissions).
     */
    @Bean(name = "defenseDataSourceProperties")
    @ConfigurationProperties("datasource.defensedb")
    public DataSourceProperties defenseDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Secondary DataSource for defensedb with HikariCP connection pooling.
     * Used to access defense-service database for defense data.
     */
    @Bean(name = "defenseDataSource")
    @ConfigurationProperties("datasource.defensedb.hikari")
    public DataSource defenseDataSource(
            @Qualifier("defenseDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * JdbcTemplate for defensedb operations.
     */
    @Bean(name = "defenseJdbcTemplate")
    public JdbcTemplate defenseJdbcTemplate(@Qualifier("defenseDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // ========== Secondary DataSource - notificationdb ==========

    /**
     * Secondary DataSource properties for notificationdb.
     * Used for notification statistics (read-only permissions).
     */
    @Bean(name = "notificationDataSourceProperties")
    @ConfigurationProperties("datasource.notificationdb")
    public DataSourceProperties notificationDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Secondary DataSource for notificationdb with HikariCP connection pooling.
     * Used to access notification-service database for notification data.
     */
    @Bean(name = "notificationDataSource")
    @ConfigurationProperties("datasource.notificationdb.hikari")
    public DataSource notificationDataSource(
            @Qualifier("notificationDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * JdbcTemplate for notificationdb operations.
     */
    @Bean(name = "notificationJdbcTemplate")
    public JdbcTemplate notificationJdbcTemplate(
            @Qualifier("notificationDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
