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
 * Configuration for reading enrollment records that are candidates for
 * archiving.
 * Identifies enrollments with status VALIDÉ or REJETÉ that are older than 1
 * year
 * and have not yet been archived.
 */
@Configuration
public class EnrollmentArchiveCandidateReaderConfig {

    /**
     * Creates a JdbcCursorItemReader for enrollment archive candidates.
     * 
     * Query criteria:
     * - Status is VALIDÉ or REJETÉ
     * - Validation/rejection date is more than 1 year ago
     * - Not yet archived (archived flag is false or null)
     * 
     * @param inscriptionDataSource DataSource for inscription database
     * @return Configured JdbcCursorItemReader for enrollment archive candidates
     */
    @Bean
    public JdbcCursorItemReader<Inscription> enrollmentArchiveCandidateReader(
            @Qualifier("inscriptionDataSource") DataSource inscriptionDataSource) {

        String sql = "SELECT " +
                "    i.id, " +
                "    i.doctorant_id, " +
                "    i.status, " +
                "    i.date_validation, " +
                "    i.date_rejection, " +
                "    i.motif_refus, " +
                "    i.date_premiere_inscription, " +
                "    i.annee_universitaire, " +
                "    i.discipline, " +
                "    i.laboratoire, " +
                "    i.directeur_these_id, " +
                "    i.co_directeur_these_id, " +
                "    i.sujet_these, " +
                "    i.has_derogation, " +
                "    i.derogation_motif, " +
                "    i.derogation_date, " +
                "    i.archived " +
                "FROM inscription i " +
                "WHERE i.status IN ('VALIDÉ', 'REJETÉ') " +
                "  AND (i.archived IS NULL OR i.archived = false) " +
                "  AND ( " +
                "    (i.status = 'VALIDÉ' AND i.date_validation < DATE_SUB(CURDATE(), INTERVAL 1 YEAR)) " +
                "    OR " +
                "    (i.status = 'REJETÉ' AND i.date_rejection < DATE_SUB(CURDATE(), INTERVAL 1 YEAR)) " +
                "  ) " +
                "ORDER BY i.id";

        return new JdbcCursorItemReaderBuilder<Inscription>()
                .name("enrollmentArchiveCandidateReader")
                .dataSource(inscriptionDataSource)
                .sql(sql)
                .rowMapper(new EnrollmentRowMapper())
                .fetchSize(20)
                .build();
    }

    /**
     * RowMapper for converting database rows to Inscription objects.
     */
    private static class EnrollmentRowMapper implements RowMapper<Inscription> {
        @Override
        public Inscription mapRow(ResultSet rs, int rowNum) throws SQLException {
            Inscription inscription = new Inscription();
            inscription.setId(rs.getLong("id"));
            inscription.setDoctorantId(rs.getLong("doctorant_id"));
            inscription.setStatus(rs.getString("status"));

            // Handle nullable date fields
            java.sql.Date dateValidation = rs.getDate("date_validation");
            if (dateValidation != null) {
                inscription.setDateValidation(dateValidation.toLocalDate());
            }

            java.sql.Date dateRejection = rs.getDate("date_rejection");
            if (dateRejection != null) {
                inscription.setDateRejection(dateRejection.toLocalDate());
            }

            inscription.setMotifRefus(rs.getString("motif_refus"));

            java.sql.Date datePremiereInscription = rs.getDate("date_premiere_inscription");
            if (datePremiereInscription != null) {
                inscription.setDatePremiereInscription(datePremiereInscription.toLocalDate());
            }

            inscription.setAnneeUniversitaire(rs.getString("annee_universitaire"));
            inscription.setDiscipline(rs.getString("discipline"));
            inscription.setLaboratoire(rs.getString("laboratoire"));

            Long directeurId = rs.getLong("directeur_these_id");
            if (!rs.wasNull()) {
                inscription.setDirecteurTheseId(directeurId);
            }

            Long coDirecteurId = rs.getLong("co_directeur_these_id");
            if (!rs.wasNull()) {
                inscription.setCoDirecteurTheseId(coDirecteurId);
            }

            inscription.setSujetThese(rs.getString("sujet_these"));
            inscription.setHasDerogation(rs.getBoolean("has_derogation"));
            inscription.setDerogationMotif(rs.getString("derogation_motif"));

            java.sql.Date derogationDate = rs.getDate("derogation_date");
            if (derogationDate != null) {
                inscription.setDerogationDate(derogationDate.toLocalDate());
            }

            inscription.setArchived(rs.getBoolean("archived"));

            return inscription;
        }
    }
}
