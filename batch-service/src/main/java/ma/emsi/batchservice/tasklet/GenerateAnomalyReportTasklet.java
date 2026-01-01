package ma.emsi.batchservice.tasklet;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Tasklet for generating anomaly report PDF when data consistency issues are
 * detected.
 * 
 * Report Generation:
 * - Check if anomalies were detected in previous steps
 * - If yes, generate PDF with anomaly details
 * - Include anomaly type, count, and corrective actions
 * - Send report via email to technical admin
 * 
 * Report Sections:
 * - Executive Summary
 * - User-Enrollment Anomalies
 * - Enrollment-Defense Anomalies
 * - Role Synchronization Changes
 * - Orphaned Documents
 * - Notification Retry Results
 * - Recommended Actions
 * 
 * Requirements: 5.13, 5.14
 */
@Slf4j
@Component
public class GenerateAnomalyReportTasklet implements Tasklet {

    @Value("${batch.reports.directory:./reports}")
    private String reportsDirectory;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public GenerateAnomalyReportTasklet(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Checking if anomaly report generation is needed");

        // Retrieve metrics from execution context
        var executionContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();

        int userEnrollmentAnomalies = executionContext.getInt("userEnrollmentAnomalies", 0);
        int enrollmentDefenseAnomalies = executionContext.getInt("enrollmentDefenseAnomalies", 0);
        int rolesAdded = executionContext.getInt("rolesAdded", 0);
        int rolesTransitioned = executionContext.getInt("rolesTransitioned", 0);
        int orphanedDocuments = executionContext.getInt("orphanedDocuments", 0);
        int notificationRetryFailure = executionContext.getInt("notificationRetryFailure", 0);

        int totalAnomalies = userEnrollmentAnomalies + enrollmentDefenseAnomalies +
                rolesAdded + rolesTransitioned + orphanedDocuments + notificationRetryFailure;

        if (totalAnomalies == 0) {
            log.info("No anomalies detected, skipping report generation");
            return RepeatStatus.FINISHED;
        }

        log.info("Anomalies detected, generating anomaly report. Total: {}", totalAnomalies);

        // Create reports directory if it doesn't exist
        File reportsDir = new File(reportsDirectory);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        // Generate filename: anomaly_report_YYYY_MM_DD_HHmmss.pdf
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss"));
        String filename = "anomaly_report_" + timestamp + ".pdf";
        String filepath = reportsDirectory + File.separator + filename;

        // Create PDF document
        try (PdfWriter writer = new PdfWriter(new FileOutputStream(filepath));
                PdfDocument pdfDoc = new PdfDocument(writer);
                Document document = new Document(pdfDoc)) {

            // Add header
            addHeader(document);

            // Add executive summary
            addExecutiveSummary(document, totalAnomalies, userEnrollmentAnomalies,
                    enrollmentDefenseAnomalies, rolesAdded, rolesTransitioned,
                    orphanedDocuments, notificationRetryFailure);

            // Add detailed sections
            if (userEnrollmentAnomalies > 0) {
                addUserEnrollmentSection(document, userEnrollmentAnomalies,
                        executionContext.getInt("userEnrollmentCorrected", 0));
            }

            if (enrollmentDefenseAnomalies > 0) {
                addEnrollmentDefenseSection(document, enrollmentDefenseAnomalies,
                        executionContext.getInt("enrollmentDefenseCorrected", 0));
            }

            if (rolesAdded > 0 || rolesTransitioned > 0) {
                addRoleSynchronizationSection(document, rolesAdded, rolesTransitioned);
            }

            if (orphanedDocuments > 0) {
                addOrphanedDocumentsSection(document, orphanedDocuments,
                        executionContext.getString("orphanedFilesList", ""));
            }

            if (notificationRetryFailure > 0) {
                addNotificationRetrySection(document, notificationRetryFailure,
                        executionContext.getInt("notificationRetrySuccess", 0));
            }

            // Add recommended actions
            addRecommendedActions(document);

            // Add footer
            addFooter(document);

            log.info("Anomaly report generated successfully: {}", filepath);
        }

        // Store report path in execution context
        executionContext.putString("anomalyReportPath", filepath);

        // Send report to technical admin via Kafka
        sendReportNotification(filepath, totalAnomalies);

        return RepeatStatus.FINISHED;
    }

    private void addHeader(Document document) {
        Paragraph title = new Paragraph("Data Consistency Anomaly Report")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.RED);
        document.add(title);

        Paragraph subtitle = new Paragraph("Generated: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subtitle);
    }

    private void addExecutiveSummary(Document document, int totalAnomalies,
            int userEnrollmentAnomalies, int enrollmentDefenseAnomalies,
            int rolesAdded, int rolesTransitioned,
            int orphanedDocuments, int notificationRetryFailure) {
        document.add(new Paragraph("Executive Summary")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10));

        document.add(new Paragraph("Total anomalies detected: " + totalAnomalies)
                .setFontSize(12)
                .setBold()
                .setFontColor(ColorConstants.RED));

        Table summaryTable = new Table(UnitValue.createPercentArray(new float[] { 3, 1 }))
                .useAllAvailableWidth()
                .setMarginTop(10);

        summaryTable.addHeaderCell("Anomaly Type");
        summaryTable.addHeaderCell("Count");

        if (userEnrollmentAnomalies > 0) {
            summaryTable.addCell("User-Enrollment Inconsistencies");
            summaryTable.addCell(String.valueOf(userEnrollmentAnomalies));
        }

        if (enrollmentDefenseAnomalies > 0) {
            summaryTable.addCell("Enrollment-Defense Inconsistencies");
            summaryTable.addCell(String.valueOf(enrollmentDefenseAnomalies));
        }

        if (rolesAdded > 0) {
            summaryTable.addCell("Missing Roles Added");
            summaryTable.addCell(String.valueOf(rolesAdded));
        }

        if (rolesTransitioned > 0) {
            summaryTable.addCell("Roles Transitioned (Doctorate Completion)");
            summaryTable.addCell(String.valueOf(rolesTransitioned));
        }

        if (orphanedDocuments > 0) {
            summaryTable.addCell("Orphaned Documents");
            summaryTable.addCell(String.valueOf(orphanedDocuments));
        }

        if (notificationRetryFailure > 0) {
            summaryTable.addCell("Failed Notification Retries");
            summaryTable.addCell(String.valueOf(notificationRetryFailure));
        }

        document.add(summaryTable);
    }

    private void addUserEnrollmentSection(Document document, int anomalies, int corrected) {
        document.add(new Paragraph("User-Enrollment Inconsistencies")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20));

        document.add(new Paragraph("Anomalies Detected: " + anomalies)
                .setFontSize(12));
        document.add(new Paragraph("Corrective Actions Applied: " + corrected)
                .setFontSize(12));

        document.add(new Paragraph("Description: Enrollments were found that reference non-existent users.")
                .setFontSize(10)
                .setMarginTop(5));
        document.add(new Paragraph("Corrective Action: Affected enrollments have been marked as SUSPENDU.")
                .setFontSize(10));
        document.add(new Paragraph("Manual Action Required: Review suspended enrollments and contact affected users.")
                .setFontSize(10)
                .setFontColor(ColorConstants.ORANGE));
    }

    private void addEnrollmentDefenseSection(Document document, int anomalies, int corrected) {
        document.add(new Paragraph("Enrollment-Defense Inconsistencies")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20));

        document.add(new Paragraph("Anomalies Detected: " + anomalies)
                .setFontSize(12));
        document.add(new Paragraph("Corrective Actions Applied: " + corrected)
                .setFontSize(12));

        document.add(
                new Paragraph("Description: Defense requests were found with invalid or non-validated enrollments.")
                        .setFontSize(10)
                        .setMarginTop(5));
        document.add(new Paragraph("Corrective Action: Affected defense requests have been marked as BLOQUÉ.")
                .setFontSize(10));
        document.add(new Paragraph("Manual Action Required: Review blocked defenses and validate enrollments.")
                .setFontSize(10)
                .setFontColor(ColorConstants.ORANGE));
    }

    private void addRoleSynchronizationSection(Document document, int rolesAdded, int rolesTransitioned) {
        document.add(new Paragraph("Role Synchronization")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20));

        document.add(new Paragraph("Roles Added: " + rolesAdded)
                .setFontSize(12));
        document.add(new Paragraph("Roles Transitioned: " + rolesTransitioned)
                .setFontSize(12));

        document.add(
                new Paragraph("Description: User roles have been synchronized based on enrollment and defense status.")
                        .setFontSize(10)
                        .setMarginTop(5));
        document.add(new Paragraph("Actions Taken: ROLE_DOCTORANT_ACTIF added to validated enrollments, " +
                "ROLE_DOCTEUR assigned to completed defenses.")
                .setFontSize(10));
    }

    private void addOrphanedDocumentsSection(Document document, int orphanedCount, String filesList) {
        document.add(new Paragraph("Orphaned Documents")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20));

        document.add(new Paragraph("Orphaned Files Found: " + orphanedCount)
                .setFontSize(12));

        document.add(new Paragraph("Description: Files were found in uploads directory without database references.")
                .setFontSize(10)
                .setMarginTop(5));
        document.add(new Paragraph("Corrective Action: Files have been moved to uploads/orphelins directory.")
                .setFontSize(10));

        if (filesList != null && !filesList.isEmpty()) {
            document.add(new Paragraph("Files: " + filesList)
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY));
        }

        document.add(new Paragraph("Manual Action Required: Review orphaned files and delete if not needed.")
                .setFontSize(10)
                .setFontColor(ColorConstants.ORANGE));
    }

    private void addNotificationRetrySection(Document document, int failures, int successes) {
        document.add(new Paragraph("Notification Retry Results")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20));

        document.add(new Paragraph("Retry Successes: " + successes)
                .setFontSize(12));
        document.add(new Paragraph("Retry Failures: " + failures)
                .setFontSize(12)
                .setFontColor(ColorConstants.RED));

        document.add(new Paragraph("Description: Stale pending notifications (>24h) were retried.")
                .setFontSize(10)
                .setMarginTop(5));
        document.add(new Paragraph("Corrective Action: Failed notifications have been marked as FAILED.")
                .setFontSize(10));
        document.add(new Paragraph("Manual Action Required: Investigate failed notifications and resend manually.")
                .setFontSize(10)
                .setFontColor(ColorConstants.ORANGE));
    }

    private void addRecommendedActions(Document document) {
        document.add(new Paragraph("Recommended Actions")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20));

        document.add(new Paragraph("1. Review all suspended enrollments and blocked defenses")
                .setFontSize(10));
        document.add(new Paragraph("2. Verify orphaned documents and clean up if necessary")
                .setFontSize(10));
        document.add(new Paragraph("3. Investigate failed notification retries")
                .setFontSize(10));
        document.add(new Paragraph("4. Monitor role synchronization for accuracy")
                .setFontSize(10));
        document.add(new Paragraph("5. Schedule follow-up consistency check in 24 hours")
                .setFontSize(10));
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("End of Report")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30)
                .setFontColor(ColorConstants.GRAY));
    }

    private void sendReportNotification(String reportPath, int totalAnomalies) {
        try {
            Map<String, Object> notification = Map.of(
                    "type", "ANOMALY_REPORT_GENERATED",
                    "reportPath", reportPath,
                    "totalAnomalies", totalAnomalies,
                    "message", "Data consistency anomaly report has been generated",
                    "priority", "HIGH",
                    "recipientRole", "TECHNICAL_ADMIN",
                    "timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send("notifications", "anomaly-report", notification);
            log.info("Anomaly report notification sent to technical admin");
        } catch (Exception e) {
            log.error("Failed to send anomaly report notification", e);
            // Don't fail the tasklet if notification fails
        }
    }
}
