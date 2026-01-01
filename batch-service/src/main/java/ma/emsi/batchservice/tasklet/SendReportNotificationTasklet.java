package ma.emsi.batchservice.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tasklet to send monthly report notifications to administrators.
 * Sends email with PDF attachment and publishes Kafka event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendReportNotificationTasklet implements Tasklet {

    @Qualifier("userJdbcTemplate")
    private final JdbcTemplate userJdbcTemplate;

    private final JavaMailSender mailSender;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting report notification sending...");

        // Retrieve PDF path from execution context
        var executionContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();

        String pdfFilePath = (String) executionContext.get("pdfFilePath");
        String pdfFileName = (String) executionContext.get("pdfFileName");

        if (pdfFilePath == null || pdfFileName == null) {
            throw new IllegalStateException("PDF file path not found in execution context");
        }

        File pdfFile = new File(pdfFilePath);
        if (!pdfFile.exists()) {
            throw new IllegalStateException("PDF file does not exist: " + pdfFilePath);
        }

        // Query all admin users
        List<Map<String, Object>> adminUsers = userJdbcTemplate.queryForList(
                "SELECT DISTINCT u.id, u.email, u.nom, u.prenom " +
                        "FROM user u " +
                        "JOIN user_roles ur ON u.id = ur.user_id " +
                        "JOIN role r ON ur.role_id = r.id " +
                        "WHERE r.name = 'ROLE_ADMIN' AND u.account_status = 'ACTIVE'");

        log.info("Found {} admin users to notify", adminUsers.size());

        int successCount = 0;
        int failureCount = 0;

        // Send email to each admin
        for (Map<String, Object> admin : adminUsers) {
            String email = (String) admin.get("email");
            String nom = (String) admin.get("nom");
            String prenom = (String) admin.get("prenom");

            try {
                sendEmailWithAttachment(email, nom, prenom, pdfFile, pdfFileName);
                successCount++;
                log.info("Report sent successfully to: {}", email);
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to send report to: {}", email, e);
            }
        }

        // Publish Kafka event
        try {
            Map<String, Object> reportEvent = new HashMap<>();
            reportEvent.put("type", "MONTHLY_REPORT_GENERATED");
            reportEvent.put("fileName", pdfFileName);
            reportEvent.put("filePath", pdfFilePath);
            reportEvent.put("fileSize", pdfFile.length());
            reportEvent.put("recipientCount", adminUsers.size());
            reportEvent.put("successCount", successCount);
            reportEvent.put("failureCount", failureCount);
            reportEvent.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send("notifications", "monthly-report", reportEvent);
            log.info("Kafka event published for monthly report");
        } catch (Exception e) {
            log.error("Failed to publish Kafka event for monthly report", e);
        }

        // Store send status in execution context
        executionContext.put("emailSuccessCount", successCount);
        executionContext.put("emailFailureCount", failureCount);

        log.info("Report notification sending completed: {} success, {} failures", successCount, failureCount);

        return RepeatStatus.FINISHED;
    }

    private void sendEmailWithAttachment(String toEmail, String nom, String prenom,
            File attachment, String attachmentName) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("Rapport Mensuel - Gestion Doctorale");

        String emailBody = String.format(
                "Bonjour %s %s,\n\n" +
                        "Veuillez trouver ci-joint le rapport mensuel de la plateforme de gestion doctorale.\n\n" +
                        "Ce rapport contient les statistiques détaillées des inscriptions, soutenances, " +
                        "notifications et utilisateurs pour le mois précédent.\n\n" +
                        "Cordialement,\n" +
                        "Système de Gestion Doctorale",
                prenom, nom);

        helper.setText(emailBody);

        // Attach PDF
        FileSystemResource file = new FileSystemResource(attachment);
        helper.addAttachment(attachmentName, file);

        mailSender.send(message);
    }
}
