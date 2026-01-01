package ma.emsi.batchservice.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tasklet for verifying and synchronizing user roles based on enrollment and
 * defense status.
 * 
 * Role Synchronization Rules:
 * 1. Doctorants with validated enrollments should have ROLE_DOCTORANT_ACTIF
 * 2. Doctorants with completed defenses should have ROLE_DOCTEUR (not
 * ROLE_DOCTORANT_ACTIF)
 * 
 * Corrective Actions:
 * - Add ROLE_DOCTORANT_ACTIF to users with validated enrollments if missing
 * - Remove ROLE_DOCTORANT_ACTIF and add ROLE_DOCTEUR for users with completed
 * defenses
 * - Log all role changes for audit trail
 * 
 * Requirements: 5.6, 5.7
 */
@Slf4j
@Component
public class VerifyUserRolesTasklet implements Tasklet {

    private final JdbcTemplate userJdbcTemplate;
    private final JdbcTemplate inscriptionJdbcTemplate;
    private final JdbcTemplate defenseJdbcTemplate;

    public VerifyUserRolesTasklet(
            @Qualifier("userJdbcTemplate") JdbcTemplate userJdbcTemplate,
            @Qualifier("inscriptionJdbcTemplate") JdbcTemplate inscriptionJdbcTemplate,
            @Qualifier("defenseJdbcTemplate") JdbcTemplate defenseJdbcTemplate) {
        this.userJdbcTemplate = userJdbcTemplate;
        this.inscriptionJdbcTemplate = inscriptionJdbcTemplate;
        this.defenseJdbcTemplate = defenseJdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting user roles verification and synchronization");

        int rolesAdded = 0;
        int rolesRemoved = 0;
        int rolesTransitioned = 0;

        try {
            // Step 1: Add ROLE_DOCTORANT_ACTIF to users with validated enrollments
            rolesAdded = addDoctorantActifRole();

            // Step 2: Transition users with completed defenses to ROLE_DOCTEUR
            rolesTransitioned = transitionToRoleDocteur();
            rolesRemoved = rolesTransitioned; // Each transition removes ROLE_DOCTORANT_ACTIF

            // Store metrics in execution context for listener
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("rolesAdded", rolesAdded);
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("rolesRemoved", rolesRemoved);
            chunkContext.getStepContext()
                    .getStepExecution()
                    .getExecutionContext()
                    .putInt("rolesTransitioned", rolesTransitioned);

            log.info("User roles verification completed. Added: {}, Removed: {}, Transitioned: {}",
                    rolesAdded, rolesRemoved, rolesTransitioned);

        } catch (Exception e) {
            log.error("Error during user roles verification", e);
            throw e;
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * Adds ROLE_DOCTORANT_ACTIF to users with validated enrollments who don't have
     * it.
     */
    private int addDoctorantActifRole() {
        log.info("Checking for users with validated enrollments missing ROLE_DOCTORANT_ACTIF");

        // Find doctorants with validated enrollments
        String query = """
                SELECT DISTINCT doctorant_id
                FROM inscription
                WHERE statut = 'VALIDÉ'
                """;

        List<Long> doctorantIds = inscriptionJdbcTemplate.queryForList(query, Long.class);
        log.info("Found {} doctorants with validated enrollments", doctorantIds.size());

        int rolesAdded = 0;

        // Get ROLE_DOCTORANT_ACTIF role ID
        Long roleId = getRoleId("ROLE_DOCTORANT_ACTIF");
        if (roleId == null) {
            log.warn("ROLE_DOCTORANT_ACTIF not found in database, skipping role addition");
            return 0;
        }

        for (Long doctorantId : doctorantIds) {
            // Check if user already has the role
            String checkRoleQuery = """
                    SELECT COUNT(*)
                    FROM user_role
                    WHERE user_id = ? AND role_id = ?
                    """;
            Integer count = userJdbcTemplate.queryForObject(checkRoleQuery, Integer.class, doctorantId, roleId);

            if (count == null || count == 0) {
                // User doesn't have the role, add it
                String insertRoleQuery = """
                        INSERT INTO user_role (user_id, role_id)
                        VALUES (?, ?)
                        """;
                try {
                    userJdbcTemplate.update(insertRoleQuery, doctorantId, roleId);
                    rolesAdded++;
                    log.info("Added ROLE_DOCTORANT_ACTIF to user {}", doctorantId);
                } catch (Exception e) {
                    log.warn("Failed to add ROLE_DOCTORANT_ACTIF to user {}: {}", doctorantId, e.getMessage());
                }
            }
        }

        log.info("Added ROLE_DOCTORANT_ACTIF to {} users", rolesAdded);
        return rolesAdded;
    }

    /**
     * Transitions users with completed defenses from ROLE_DOCTORANT_ACTIF to
     * ROLE_DOCTEUR.
     */
    private int transitionToRoleDocteur() {
        log.info("Checking for users with completed defenses to transition to ROLE_DOCTEUR");

        // Find doctorants with successfully completed defenses
        String query = """
                SELECT DISTINCT i.doctorant_id
                FROM defense d
                JOIN inscription i ON d.inscription_id = i.id
                WHERE d.statut = 'TERMINÉ'
                AND d.mention IN ('HONORABLE', 'TRES_HONORABLE', 'TRES_HONORABLE_AVEC_FELICITATIONS')
                """;

        List<Long> doctorantIds = defenseJdbcTemplate.queryForList(query, Long.class);
        log.info("Found {} doctorants with completed defenses", doctorantIds.size());

        int rolesTransitioned = 0;

        // Get role IDs
        Long roleDoctorantActifId = getRoleId("ROLE_DOCTORANT_ACTIF");
        Long roleDocteurId = getRoleId("ROLE_DOCTEUR");

        if (roleDocteurId == null) {
            log.warn("ROLE_DOCTEUR not found in database, skipping role transition");
            return 0;
        }

        for (Long doctorantId : doctorantIds) {
            boolean transitioned = false;

            // Remove ROLE_DOCTORANT_ACTIF if present
            if (roleDoctorantActifId != null) {
                String deleteRoleQuery = """
                        DELETE FROM user_role
                        WHERE user_id = ? AND role_id = ?
                        """;
                try {
                    int deleted = userJdbcTemplate.update(deleteRoleQuery, doctorantId, roleDoctorantActifId);
                    if (deleted > 0) {
                        log.debug("Removed ROLE_DOCTORANT_ACTIF from user {}", doctorantId);
                        transitioned = true;
                    }
                } catch (Exception e) {
                    log.warn("Failed to remove ROLE_DOCTORANT_ACTIF from user {}: {}", doctorantId, e.getMessage());
                }
            }

            // Add ROLE_DOCTEUR if not present
            String checkRoleQuery = """
                    SELECT COUNT(*)
                    FROM user_role
                    WHERE user_id = ? AND role_id = ?
                    """;
            Integer count = userJdbcTemplate.queryForObject(checkRoleQuery, Integer.class, doctorantId, roleDocteurId);

            if (count == null || count == 0) {
                String insertRoleQuery = """
                        INSERT INTO user_role (user_id, role_id)
                        VALUES (?, ?)
                        """;
                try {
                    userJdbcTemplate.update(insertRoleQuery, doctorantId, roleDocteurId);
                    log.info("Added ROLE_DOCTEUR to user {}", doctorantId);
                    transitioned = true;
                } catch (Exception e) {
                    log.warn("Failed to add ROLE_DOCTEUR to user {}: {}", doctorantId, e.getMessage());
                }
            }

            if (transitioned) {
                rolesTransitioned++;
            }
        }

        log.info("Transitioned {} users to ROLE_DOCTEUR", rolesTransitioned);
        return rolesTransitioned;
    }

    /**
     * Retrieves role ID by role name.
     */
    private Long getRoleId(String roleName) {
        try {
            String query = "SELECT id FROM role WHERE name = ?";
            return userJdbcTemplate.queryForObject(query, Long.class, roleName);
        } catch (Exception e) {
            log.warn("Failed to retrieve role ID for {}: {}", roleName, e.getMessage());
            return null;
        }
    }
}
