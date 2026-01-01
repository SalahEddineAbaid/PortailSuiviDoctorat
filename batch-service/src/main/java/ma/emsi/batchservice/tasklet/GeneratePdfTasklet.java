package ma.emsi.batchservice.tasklet;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.dto.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Tasklet to generate PDF monthly report from collected statistics.
 * Creates a comprehensive PDF document with charts, tables, and KPI dashboard.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeneratePdfTasklet implements Tasklet {

    @Value("${batch.reports.directory:./reports}")
    private String reportsDirectory;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting PDF report generation...");

        // Retrieve statistics from execution context
        var executionContext = chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext();

        EnrollmentStatsDTO enrollmentStats = (EnrollmentStatsDTO) executionContext.get("enrollmentStats");
        DefenseStatsDTO defenseStats = (DefenseStatsDTO) executionContext.get("defenseStats");
        NotificationStatsDTO notificationStats = (NotificationStatsDTO) executionContext.get("notificationStats");
        UserStatsDTO userStats = (UserStatsDTO) executionContext.get("userStats");

        // Calculate report month
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        LocalDate reportMonth = previousMonth.atDay(1);

        // Create reports directory if it doesn't exist
        File reportsDir = new File(reportsDirectory);
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }

        // Generate filename: rapport_YYYY_MM.pdf
        String filename = String.format("rapport_%d_%02d.pdf",
                previousMonth.getYear(), previousMonth.getMonthValue());
        String filepath = reportsDirectory + File.separator + filename;

        // Create PDF document
        PdfWriter writer = new PdfWriter(new FileOutputStream(filepath));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Add header
        addHeader(document, reportMonth);

        // Add KPI dashboard
        addKpiDashboard(document, enrollmentStats, defenseStats, notificationStats, userStats);

        // Add enrollment section with chart
        addEnrollmentSection(document, enrollmentStats);

        // Add defense section with chart
        addDefenseSection(document, defenseStats);

        // Add notification section
        addNotificationSection(document, notificationStats);

        // Add user section
        addUserSection(document, userStats);

        // Add alerts section if anomalies exist
        addAlertsSection(document, enrollmentStats, defenseStats, notificationStats);

        // Add footer
        addFooter(document);

        // Close document
        document.close();

        // Store PDF path in execution context
        executionContext.put("pdfFilePath", filepath);
        executionContext.put("pdfFileName", filename);

        log.info("PDF report generated successfully: {}", filepath);

        return RepeatStatus.FINISHED;
    }

    private void addHeader(Document document, LocalDate reportMonth) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");

        Paragraph title = new Paragraph("Rapport Mensuel - Gestion Doctorale")
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        Paragraph subtitle = new Paragraph("Période: " + reportMonth.format(formatter))
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subtitle);
    }

    private void addKpiDashboard(Document document, EnrollmentStatsDTO enrollmentStats,
            DefenseStatsDTO defenseStats, NotificationStatsDTO notificationStats,
            UserStatsDTO userStats) {
        document.add(new Paragraph("Tableau de Bord KPI")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10));

        Table kpiTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 1, 1 }))
                .useAllAvailableWidth();

        // Header row
        kpiTable.addHeaderCell("Inscriptions");
        kpiTable.addHeaderCell("Soutenances");
        kpiTable.addHeaderCell("Notifications");
        kpiTable.addHeaderCell("Utilisateurs");

        // Data row
        kpiTable.addCell(String.valueOf(enrollmentStats.getTotalEnrollments()));
        kpiTable.addCell(String.valueOf(defenseStats.getCompletedDefensesCount()));
        kpiTable.addCell(String.valueOf(notificationStats.getTotalNotificationsSent()));
        kpiTable.addCell(String.valueOf(userStats.getTotalActiveUsers()));

        document.add(kpiTable);
        document.add(new Paragraph("\n"));
    }

    private void addEnrollmentSection(Document document, EnrollmentStatsDTO stats) throws Exception {
        document.add(new Paragraph("Statistiques des Inscriptions")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10));

        // Create status distribution chart
        if (stats.getStatusDistribution() != null && !stats.getStatusDistribution().isEmpty()) {
            File chartFile = createPieChart(stats.getStatusDistribution(),
                    "Distribution par Statut", "enrollment_status_chart.png");
            Image chartImage = new Image(ImageDataFactory.create(chartFile.getAbsolutePath()));
            chartImage.setWidth(400);
            document.add(chartImage);
            chartFile.delete();
        }

        // Add detailed table
        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                .useAllAvailableWidth();

        table.addCell("Total Inscriptions").addCell(String.valueOf(stats.getTotalEnrollments()));
        table.addCell("Réinscriptions").addCell(String.valueOf(stats.getReinscriptionsCount()));
        table.addCell("Dérogations Demandées").addCell(String.valueOf(stats.getDerogationsRequested()));
        table.addCell("Dérogations Accordées").addCell(String.valueOf(stats.getDerogationsGranted()));
        table.addCell("Temps Moyen de Traitement (jours)")
                .addCell(String.format("%.2f", stats.getAverageProcessingTimeDays()));
        table.addCell("Taux Validation Directeur")
                .addCell(String.format("%.2f%%", stats.getDirectorValidationRate()));
        table.addCell("Taux Validation Admin")
                .addCell(String.format("%.2f%%", stats.getAdminValidationRate()));

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addDefenseSection(Document document, DefenseStatsDTO stats) throws Exception {
        document.add(new Paragraph("Statistiques des Soutenances")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10));

        // Create mention distribution chart
        if (stats.getMentionDistribution() != null && !stats.getMentionDistribution().isEmpty()) {
            File chartFile = createBarChart(stats.getMentionDistribution(),
                    "Distribution des Mentions", "defense_mention_chart.png");
            Image chartImage = new Image(ImageDataFactory.create(chartFile.getAbsolutePath()));
            chartImage.setWidth(400);
            document.add(chartImage);
            chartFile.delete();
        }

        // Add detailed table
        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                .useAllAvailableWidth();

        table.addCell("Demandes de Soutenance").addCell(String.valueOf(stats.getDefenseRequestsCount()));
        table.addCell("Soutenances Complétées").addCell(String.valueOf(stats.getCompletedDefensesCount()));
        table.addCell("Jurys Formés").addCell(String.valueOf(stats.getJuryCount()));
        table.addCell("Rapports Soumis").addCell(String.valueOf(stats.getSubmittedReportsCount()));
        table.addCell("Taux Acceptation Jury")
                .addCell(String.format("%.2f%%", stats.getJuryMemberAcceptanceRate()));
        table.addCell("Temps Moyen Demande→Autorisation (jours)")
                .addCell(String.format("%.2f", stats.getAverageTimeRequestToAuthorizationDays()));
        table.addCell("Temps Moyen Autorisation→Soutenance (jours)")
                .addCell(String.format("%.2f", stats.getAverageTimeAuthorizationToDefenseDays()));

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addNotificationSection(Document document, NotificationStatsDTO stats) {
        document.add(new Paragraph("Statistiques des Notifications")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10));

        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                .useAllAvailableWidth();

        table.addCell("Total Notifications").addCell(String.valueOf(stats.getTotalNotificationsSent()));
        table.addCell("Taux de Succès").addCell(String.format("%.2f%%", stats.getSuccessRate()));
        table.addCell("Notifications Échouées").addCell(String.valueOf(stats.getFailedNotificationsCount()));
        table.addCell("Temps Moyen d'Envoi (ms)")
                .addCell(String.format("%.2f", stats.getAverageSendTimeMs()));

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addUserSection(Document document, UserStatsDTO stats) {
        document.add(new Paragraph("Statistiques des Utilisateurs")
                .setFontSize(16)
                .setBold()
                .setMarginTop(10));

        Table table = new Table(UnitValue.createPercentArray(new float[] { 1, 1 }))
                .useAllAvailableWidth();

        table.addCell("Utilisateurs Actifs").addCell(String.valueOf(stats.getTotalActiveUsers()));
        table.addCell("Nouveaux Utilisateurs").addCell(String.valueOf(stats.getNewUsersCount()));
        table.addCell("Taux de Connexion").addCell(String.format("%.2f%%", stats.getConnectionRate()));

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addAlertsSection(Document document, EnrollmentStatsDTO enrollmentStats,
            DefenseStatsDTO defenseStats, NotificationStatsDTO notificationStats) {
        boolean hasAlerts = false;
        StringBuilder alertsText = new StringBuilder();

        // Check for low validation rates
        if (enrollmentStats.getDirectorValidationRate() < 50) {
            hasAlerts = true;
            alertsText.append("⚠ Taux de validation directeur faible: ")
                    .append(String.format("%.2f%%\n", enrollmentStats.getDirectorValidationRate()));
        }

        // Check for low notification success rate
        if (notificationStats.getSuccessRate() < 90) {
            hasAlerts = true;
            alertsText.append("⚠ Taux de succès notifications faible: ")
                    .append(String.format("%.2f%%\n", notificationStats.getSuccessRate()));
        }

        // Check for low jury acceptance rate
        if (defenseStats.getJuryMemberAcceptanceRate() < 70) {
            hasAlerts = true;
            alertsText.append("⚠ Taux d'acceptation jury faible: ")
                    .append(String.format("%.2f%%\n", defenseStats.getJuryMemberAcceptanceRate()));
        }

        if (hasAlerts) {
            document.add(new Paragraph("Alertes")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(ColorConstants.RED)
                    .setMarginTop(10));

            document.add(new Paragraph(alertsText.toString())
                    .setFontColor(ColorConstants.RED));
        }
    }

    private void addFooter(Document document) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        Paragraph footer = new Paragraph("Rapport généré le " + LocalDateTime.now().format(formatter))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(footer);
    }

    private File createPieChart(Map<String, Long> data, String title, String filename) throws Exception {
        DefaultPieDataset dataset = new DefaultPieDataset();
        data.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);

        File chartFile = new File(System.getProperty("java.io.tmpdir"), filename);
        ChartUtils.saveChartAsPNG(chartFile, chart, 600, 400);

        return chartFile;
    }

    private File createBarChart(Map<String, Long> data, String title, String filename) throws Exception {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        data.forEach((key, value) -> dataset.addValue(value, "Count", key));

        JFreeChart chart = ChartFactory.createBarChart(title, "Catégorie", "Nombre",
                dataset);

        File chartFile = new File(System.getProperty("java.io.tmpdir"), filename);
        ChartUtils.saveChartAsPNG(chartFile, chart, 600, 400);

        return chartFile;
    }
}
