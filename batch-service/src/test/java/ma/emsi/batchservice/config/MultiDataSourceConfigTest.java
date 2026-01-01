package ma.emsi.batchservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MultiDataSourceConfig.
 * Verifies that all DataSources and JdbcTemplates are properly configured.
 */
@SpringBootTest
@ActiveProfiles("test")
class MultiDataSourceConfigTest {

    @Autowired
    @Qualifier("batchDataSource")
    private DataSource batchDataSource;

    @Autowired
    @Qualifier("userDataSource")
    private DataSource userDataSource;

    @Autowired
    @Qualifier("inscriptionDataSource")
    private DataSource inscriptionDataSource;

    @Autowired
    @Qualifier("defenseDataSource")
    private DataSource defenseDataSource;

    @Autowired
    @Qualifier("notificationDataSource")
    private DataSource notificationDataSource;

    @Autowired
    @Qualifier("batchJdbcTemplate")
    private JdbcTemplate batchJdbcTemplate;

    @Autowired
    @Qualifier("userJdbcTemplate")
    private JdbcTemplate userJdbcTemplate;

    @Autowired
    @Qualifier("inscriptionJdbcTemplate")
    private JdbcTemplate inscriptionJdbcTemplate;

    @Autowired
    @Qualifier("defenseJdbcTemplate")
    private JdbcTemplate defenseJdbcTemplate;

    @Autowired
    @Qualifier("notificationJdbcTemplate")
    private JdbcTemplate notificationJdbcTemplate;

    @Test
    void testAllDataSourcesAreCreated() {
        assertThat(batchDataSource).isNotNull();
        assertThat(userDataSource).isNotNull();
        assertThat(inscriptionDataSource).isNotNull();
        assertThat(defenseDataSource).isNotNull();
        assertThat(notificationDataSource).isNotNull();
    }

    @Test
    void testAllJdbcTemplatesAreAvailable() {
        assertThat(batchJdbcTemplate).isNotNull();
        assertThat(userJdbcTemplate).isNotNull();
        assertThat(inscriptionJdbcTemplate).isNotNull();
        assertThat(defenseJdbcTemplate).isNotNull();
        assertThat(notificationJdbcTemplate).isNotNull();
    }

    @Test
    void testJdbcTemplatesHaveCorrectDataSources() {
        assertThat(batchJdbcTemplate.getDataSource()).isEqualTo(batchDataSource);
        assertThat(userJdbcTemplate.getDataSource()).isEqualTo(userDataSource);
        assertThat(inscriptionJdbcTemplate.getDataSource()).isEqualTo(inscriptionDataSource);
        assertThat(defenseJdbcTemplate.getDataSource()).isEqualTo(defenseDataSource);
        assertThat(notificationJdbcTemplate.getDataSource()).isEqualTo(notificationDataSource);
    }
}
