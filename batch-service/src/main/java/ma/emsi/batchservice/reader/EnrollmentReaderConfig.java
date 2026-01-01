package ma.emsi.batchservice.reader;

import ma.emsi.batchservice.model.Inscription;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Configuration class for enrollment readers used in duration alert job.
 * Creates three separate readers for different duration thresholds:
 * - 3-year threshold (2y9m - 3y)
 * - 6-year threshold (5y9m - 6y)
 * - Exceeded 6-year limit
 * 
 * Each reader queries active enrollments from inscriptiondb with date
 * calculations to identify doctorants at specific duration milestones.
 * 
 * Page size configured to 50 for optimal performance.
 * 
 * Requirements: 2.2, 2.4, 2.8
 */
@Configuration
public class EnrollmentReaderConfig {

    /**
     * Creates a JdbcCursorItemReader for enrollments approaching 3-year threshold.
     * 
     * Identifies active doctorants where:
     * - Current date is between first_enrollment_date + 2y9m and + 3y
     * - No dérogation has been granted
     * - Status is VALIDÉ (active enrollment)
     * 
     * @param inscriptionDataSource DataSource for inscriptiondb
     * @return Configured ItemReader for 3-year threshold enrollments
     */
    @Bean
    public JdbcCursorItemReader<Inscription> threeYearThresholdReader(
            @Qualifier("inscriptionDataSource") DataSource inscriptionDataSource) {

        // SQL query to identify enrollments approaching 3-year threshold
        // TIMESTAMPDIFF calculates months between dates
        // Between 33 months (2y9m) and 36 months (3y)
        String sql = """
                SELECT
                    i.id,
                    i.doctorant_id,
                    i.date_premiere_inscription,
                    i.statut,
                    u.email as doctorant_email,
                    CONCAT(u.nom, ' ', u.prenom) as doctorant_nom,
                    d.email as directeur_email,
                    COALESCE(i.derogation_accordee, false) as derogation_accordee,
                    COALESCE(i.derogation_exceptionnelle, false) as derogation_exceptionnelle
                FROM inscription i
                INNER JOIN user u ON i.doctorant_id = u.id
                LEFT JOIN user d ON i.directeur_id = d.id
                WHERE i.statut = 'VALIDÉ'
                  AND i.derogation_accordee = false
                  AND TIMESTAMPDIFF(MONTH, i.date_premiere_inscription, CURDATE()) >= 33
                  AND TIMESTAMPDIFF(MONTH, i.date_premiere_inscription, CURDATE()) < 36
                ORDER BY i.date_premiere_inscription ASC
                """;

        return new JdbcCursorItemReaderBuilder<Inscription>()
                .name("threeYearThresholdReader")
                .dataSource(inscriptionDataSource)
                .sql(sql)
                .rowMapper(new InscriptionRowMapper())
                .fetchSize(50)
                .build();
    }

    /**
     * Creates a JdbcCursorItemReader for enrollments approaching 6-year threshold.
     * 
     * Identifies active doctorants where:
     * - Current date is between first_enrollment_date + 5y9m and + 6y
     * - Status is VALIDÉ (active enrollment)
     * 
     * @param inscriptionDataSource DataSource for inscriptiondb
     * @return Configured ItemReader for 6-year threshold enrollments
     */
    @Bean
    public JdbcCursorItemReader<Inscription> sixYearThresholdReader(
            @Qualifier("inscriptionDataSource") DataSource inscriptionDataSource) {

        // SQL query to identify enrollments approaching 6-year threshold
        // Between 69 months (5y9m) and 72 months (6y)
        String sql = """
                SELECT
                    i.id,
                    i.doctorant_id,
                    i.date_premiere_inscription,
                    i.statut,
                    u.email as doctorant_email,
                    CONCAT(u.nom, ' ', u.prenom) as doctorant_nom,
                    d.email as directeur_email,
                    COALESCE(i.derogation_accordee, false) as derogation_accordee,
                    COALESCE(i.derogation_exceptionnelle, false) as derogation_exceptionnelle
                FROM inscription i
                INNER JOIN user u ON i.doctorant_id = u.id
                LEFT JOIN user d ON i.directeur_id = d.id
                WHERE i.statut = 'VALIDÉ'
                  AND TIMESTAMPDIFF(MONTH, i.date_premiere_inscription, CURDATE()) >= 69
                  AND TIMESTAMPDIFF(MONTH, i.date_premiere_inscription, CURDATE()) < 72
                ORDER BY i.date_premiere_inscription ASC
                """;

        return new JdbcCursorItemReaderBuilder<Inscription>()
                .name("sixYearThresholdReader")
                .dataSource(inscriptionDataSource)
                .sql(sql)
                .rowMapper(new InscriptionRowMapper())
                .fetchSize(50)
                .build();
    }

    /**
     * Creates a JdbcCursorItemReader for enrollments that have exceeded 6-year
     * limit.
     * 
     * Identifies active doctorants where:
     * - Current date is greater than first_enrollment_date + 6y
     * - No exceptional dérogation has been granted
     * - Status is VALIDÉ (active enrollment)
     * 
     * These enrollments will be marked as BLOQUÉ and urgent alerts sent.
     * 
     * @param inscriptionDataSource DataSource for inscriptiondb
     * @return Configured ItemReader for exceeded 6-year enrollments
     */
    @Bean
    public JdbcCursorItemReader<Inscription> exceededSixYearReader(
            @Qualifier("inscriptionDataSource") DataSource inscriptionDataSource) {

        // SQL query to identify enrollments that have exceeded 6-year limit
        // More than 72 months (6y) without exceptional dérogation
        String sql = """
                SELECT
                    i.id,
                    i.doctorant_id,
                    i.date_premiere_inscription,
                    i.statut,
                    u.email as doctorant_email,
                    CONCAT(u.nom, ' ', u.prenom) as doctorant_nom,
                    d.email as directeur_email,
                    COALESCE(i.derogation_accordee, false) as derogation_accordee,
                    COALESCE(i.derogation_exceptionnelle, false) as derogation_exceptionnelle
                FROM inscription i
                INNER JOIN user u ON i.doctorant_id = u.id
                LEFT JOIN user d ON i.directeur_id = d.id
                WHERE i.statut = 'VALIDÉ'
                  AND i.derogation_exceptionnelle = false
                  AND TIMESTAMPDIFF(MONTH, i.date_premiere_inscription, CURDATE()) >= 72
                ORDER BY i.date_premiere_inscription ASC
                """;

        return new JdbcCursorItemReaderBuilder<Inscription>()
                .name("exceededSixYearReader")
                .dataSource(inscriptionDataSource)
                .sql(sql)
                .rowMapper(new InscriptionRowMapper())
                .fetchSize(50)
                .build();
    }

    /**
     * RowMapper for mapping inscription result set to Inscription model.
     */
    private static class InscriptionRowMapper implements RowMapper<Inscription> {
        @Override
        public Inscription mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Inscription.builder()
                    .id(rs.getLong("id"))
                    .doctorantId(rs.getLong("doctorant_id"))
                    .datePremiereInscription(rs.getDate("date_premiere_inscription").toLocalDate())
                    .statut(rs.getString("statut"))
                    .doctorantEmail(rs.getString("doctorant_email"))
                    .doctorantNom(rs.getString("doctorant_nom"))
                    .directeurEmail(rs.getString("directeur_email"))
                    .derogationAccordee(rs.getBoolean("derogation_accordee"))
                    .derogationExceptionnelle(rs.getBoolean("derogation_exceptionnelle"))
                    .build();
        }
    }
}
