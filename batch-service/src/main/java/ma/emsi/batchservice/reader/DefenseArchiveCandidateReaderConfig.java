package ma.emsi.batchservice.reader;

import ma.emsi.batchservice.model.Defense;
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
 * Configuration for reading defense records that are candidates for archiving.
 * Identifies completed defenses that are older than 1 year, have signed PV,
 * and have not yet been archived.
 */
@Configuration
public class DefenseArchiveCandidateReaderConfig {

    /**
     * Creates a JdbcCursorItemReader for defense archive candidates.
     * 
     * Query criteria:
     * - Status is COMPLETED
     * - Defense date is more than 1 year ago
     * - Signed PV is available (pv_signed = true)
     * - Not yet archived (archived flag is false or null)
     * 
     * @param defenseDataSource DataSource for defense database
     * @return Configured JdbcCursorItemReader for defense archive candidates
     */
    @Bean
    public JdbcCursorItemReader<Defense> defenseArchiveCandidateReader(
            @Qualifier("defenseDataSource") DataSource defenseDataSource) {

        String sql = "SELECT " +
                "    d.id, " +
                "    d.inscription_id, " +
                "    d.defense_date, " +
                "    d.defense_time, " +
                "    d.location, " +
                "    d.mention, " +
                "    d.jury_id, " +
                "    d.pv_signed, " +
                "    d.pv_file_path, " +
                "    d.rapport_file_path, " +
                "    d.status, " +
                "    d.created_at, " +
                "    d.updated_at, " +
                "    d.archived " +
                "FROM defense d " +
                "WHERE d.status = 'COMPLETED' " +
                "  AND d.pv_signed = true " +
                "  AND (d.archived IS NULL OR d.archived = false) " +
                "  AND d.defense_date < DATE_SUB(CURDATE(), INTERVAL 1 YEAR) " +
                "ORDER BY d.id";

        return new JdbcCursorItemReaderBuilder<Defense>()
                .name("defenseArchiveCandidateReader")
                .dataSource(defenseDataSource)
                .sql(sql)
                .rowMapper(new DefenseRowMapper())
                .fetchSize(20)
                .build();
    }

    /**
     * RowMapper for converting database rows to Defense objects.
     */
    private static class DefenseRowMapper implements RowMapper<Defense> {
        @Override
        public Defense mapRow(ResultSet rs, int rowNum) throws SQLException {
            Defense defense = new Defense();
            defense.setId(rs.getLong("id"));
            defense.setInscriptionId(rs.getLong("inscription_id"));

            // Handle date fields
            java.sql.Date defenseDate = rs.getDate("defense_date");
            if (defenseDate != null) {
                defense.setDefenseDate(defenseDate.toLocalDate());
            }

            java.sql.Time defenseTime = rs.getTime("defense_time");
            if (defenseTime != null) {
                defense.setDefenseTime(defenseTime.toLocalTime());
            }

            defense.setLocation(rs.getString("location"));
            defense.setMention(rs.getString("mention"));

            Long juryId = rs.getLong("jury_id");
            if (!rs.wasNull()) {
                defense.setJuryId(juryId);
            }

            defense.setPvSigned(rs.getBoolean("pv_signed"));
            defense.setPvFilePath(rs.getString("pv_file_path"));
            defense.setRapportFilePath(rs.getString("rapport_file_path"));
            defense.setStatus(rs.getString("status"));

            // Handle timestamp fields
            java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
            if (createdAt != null) {
                defense.setCreatedAt(createdAt.toLocalDateTime());
            }

            java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
            if (updatedAt != null) {
                defense.setUpdatedAt(updatedAt.toLocalDateTime());
            }

            defense.setArchived(rs.getBoolean("archived"));

            return defense;
        }
    }
}
