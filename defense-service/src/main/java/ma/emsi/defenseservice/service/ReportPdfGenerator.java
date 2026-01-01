package ma.emsi.defenseservice.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.dto.response.DefenseCountByMonthDTO;
import ma.emsi.defenseservice.dto.response.StatistiquesDTO;
import ma.emsi.defenseservice.entity.Defense;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.enums.DefenseStatus;
import ma.emsi.defenseservice.enums.Mention;
import ma.emsi.defenseservice.repository.DefenseRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating statistical PDF reports (monthly and annual)
 * Implements Requirements 5.6, 5.7
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportPdfGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale FRENCH_LOCALE = Locale.FRENCH;

    private final StatistiquesService statistiquesService;
    private final DefenseRepository defenseRepository;
    private final UserServiceFacade userServiceFacade;

    /**
     * Generate a monthly report PDF for the specified month
     * Requirement 5.6
     * 
     * @param yearMonth The month for which to generate the report
     * @return PDF document as byte array
     */
    public byte[] generateMonthlyReport(YearMonth yearMonth) {
        log.info("📊 Generating monthly report for: {}", yearMonth);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // Get defenses for the specified month
            List<Defense> monthDefenses = getDefensesForMonth(yearMonth);

            // Add document sections
            addReportHeader(document, "RAPPORT MENSUEL DES SOUTENANCES", yearMonth);
            addMonthlyStatistics(document, monthDefenses, yearMonth);
            addDefenseList(document, monthDefenses, "Soutenances du mois");
            addMentionDistributionChart(document, monthDefenses);
            addReportFooter(document);

            document.close();
            log.info("✅ Monthly report generated successfully for: {}", yearMonth);

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("❌ Error generating monthly report for: {}", yearMonth, e);
            throw new RuntimeException("Failed to generate monthly report PDF", e);
        }
    }

    /**
     * Generate an annual report PDF for the specified year
     * Requirement 5.7
     * 
     * @param year The year for which to generate the report
     * @return PDF document as byte array
     */
    public byte[] generateAnnualReport(int year) {
        log.info("📊 Generating annual report for year: {}", year);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // Get defenses for the specified year
            List<Defense> yearDefenses = getDefensesForYear(year);

            // Get statistics
            StatistiquesDTO statistics = statistiquesService.getStatistiques();

            // Add document sections
            addReportHeader(document, "RAPPORT ANNUEL DES SOUTENANCES", year);
            addAnnualStatistics(document, yearDefenses, statistics, year);
            addMonthlyEvolutionChart(document, statistics);
            addDefenseList(document, yearDefenses, "Soutenances de l'année");
            addMentionDistributionChart(document, yearDefenses);
            addYearlyComparison(document, year, yearDefenses);
            addReportFooter(document);

            document.close();
            log.info("✅ Annual report generated successfully for year: {}", year);

            return baos.toByteArray();
        } catch (Exception e) {
            log.error("❌ Error generating annual report for year: {}", year, e);
            throw new RuntimeException("Failed to generate annual report PDF", e);
        }
    }

    // ==================== Header and Footer ====================

    /**
     * Add report header with title and period
     */
    private void addReportHeader(Document document, String title, Object period) {
        // Institution header
        Paragraph header = new Paragraph("UNIVERSITÉ [NOM DE L'UNIVERSITÉ]")
                .setFontSize(14)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(header);

        Paragraph subHeader = new Paragraph("École Doctorale - Service des Soutenances")
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subHeader);

        // Report title
        Paragraph reportTitle = new Paragraph(title)
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(reportTitle);

        // Period
        String periodText = period instanceof YearMonth
                ? formatYearMonth((YearMonth) period)
                : "Année " + period;

        Paragraph periodPara = new Paragraph(periodText)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30)
                .setItalic();
        document.add(periodPara);
    }

    /**
     * Add report footer with generation date
     */
    private void addReportFooter(Document document) {
        Paragraph footer = new Paragraph()
                .add("Rapport généré le " + LocalDate.now().format(DATE_FORMATTER))
                .setFontSize(9)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(20)
                .setFontColor(ColorConstants.GRAY);
        document.add(footer);
    }

    // ==================== Monthly Report Sections ====================

    /**
     * Add monthly statistics section
     */
    private void addMonthlyStatistics(Document document, List<Defense> defenses, YearMonth yearMonth) {
        addSectionTitle(document, "STATISTIQUES DU MOIS");

        Table table = new Table(UnitValue.createPercentArray(new float[] { 60, 40 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Total defenses
        addStatRow(table, "Nombre total de soutenances", String.valueOf(defenses.size()));

        // Completed defenses
        long completed = defenses.stream()
                .filter(d -> d.getStatus() == DefenseStatus.COMPLETED)
                .count();
        addStatRow(table, "Soutenances finalisées", String.valueOf(completed));

        // Success rate
        double successRate = defenses.isEmpty() ? 0.0 : (completed * 100.0) / defenses.size();
        addStatRow(table, "Taux de finalisation", String.format("%.1f%%", successRate));

        document.add(table);
    }

    // ==================== Annual Report Sections ====================

    /**
     * Add annual statistics section
     */
    private void addAnnualStatistics(Document document, List<Defense> defenses,
            StatistiquesDTO statistics, int year) {
        addSectionTitle(document, "STATISTIQUES ANNUELLES");

        Table table = new Table(UnitValue.createPercentArray(new float[] { 60, 40 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Total defenses
        addStatRow(table, "Nombre total de soutenances", String.valueOf(defenses.size()));

        // Completed defenses
        long completed = defenses.stream()
                .filter(d -> d.getStatus() == DefenseStatus.COMPLETED)
                .count();
        addStatRow(table, "Soutenances finalisées", String.valueOf(completed));

        // Success rate
        addStatRow(table, "Taux de réussite global",
                String.format("%.1f%%", statistics.getTauxReussiteGlobal()));

        // Average duration
        addStatRow(table, "Durée moyenne (années)",
                String.format("%.2f", statistics.getDureeMoyenneSoutenance()));

        // Defense requests by status
        if (statistics.getDemandesParStatut() != null) {
            addStatRow(table, "Demandes soumises",
                    String.valueOf(statistics.getDemandesParStatut()
                            .getOrDefault(DefenseRequestStatus.SUBMITTED, 0L)));
            addStatRow(table, "Demandes autorisées",
                    String.valueOf(statistics.getDemandesParStatut()
                            .getOrDefault(DefenseRequestStatus.AUTHORIZED, 0L)));
        }

        document.add(table);
    }

    /**
     * Add monthly evolution chart (text-based visualization)
     */
    private void addMonthlyEvolutionChart(Document document, StatistiquesDTO statistics) {
        addSectionTitle(document, "ÉVOLUTION MENSUELLE");

        Table table = new Table(UnitValue.createPercentArray(new float[] { 30, 20, 50 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Header
        table.addHeaderCell(createHeaderCell("Mois"));
        table.addHeaderCell(createHeaderCell("Nombre"));
        table.addHeaderCell(createHeaderCell("Visualisation"));

        // Data rows
        if (statistics.getSoutenancesParMois() != null) {
            for (DefenseCountByMonthDTO monthData : statistics.getSoutenancesParMois()) {
                String monthName = YearMonth.of(monthData.getYear(), monthData.getMonth())
                        .getMonth()
                        .getDisplayName(TextStyle.FULL, FRENCH_LOCALE);

                table.addCell(createCell(capitalize(monthName)));
                table.addCell(createCell(String.valueOf(monthData.getCount())));
                table.addCell(createCell(createBarChart(monthData.getCount(), 20)));
            }
        }

        document.add(table);
    }

    /**
     * Add yearly comparison section
     */
    private void addYearlyComparison(Document document, int year, List<Defense> currentYearDefenses) {
        addSectionTitle(document, "COMPARAISON ANNUELLE");

        // Get previous year defenses for comparison
        List<Defense> previousYearDefenses = getDefensesForYear(year - 1);

        Table table = new Table(UnitValue.createPercentArray(new float[] { 50, 25, 25 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Header
        table.addHeaderCell(createHeaderCell("Indicateur"));
        table.addHeaderCell(createHeaderCell(String.valueOf(year - 1)));
        table.addHeaderCell(createHeaderCell(String.valueOf(year)));

        // Total defenses
        addComparisonRow(table, "Nombre de soutenances",
                previousYearDefenses.size(), currentYearDefenses.size());

        // Completed defenses
        long prevCompleted = previousYearDefenses.stream()
                .filter(d -> d.getStatus() == DefenseStatus.COMPLETED)
                .count();
        long currCompleted = currentYearDefenses.stream()
                .filter(d -> d.getStatus() == DefenseStatus.COMPLETED)
                .count();
        addComparisonRow(table, "Soutenances finalisées",
                (int) prevCompleted, (int) currCompleted);

        // Calculate evolution percentage
        if (!previousYearDefenses.isEmpty()) {
            double evolution = ((currentYearDefenses.size() - previousYearDefenses.size()) * 100.0)
                    / previousYearDefenses.size();
            String evolutionText = String.format("%+.1f%%", evolution);

            Paragraph evolutionPara = new Paragraph()
                    .add("Évolution : ")
                    .add(new Paragraph(evolutionText)
                            .setFontColor(evolution >= 0 ? ColorConstants.GREEN : ColorConstants.RED)
                            .setBold())
                    .setMarginTop(10)
                    .setMarginBottom(20);
            document.add(table);
            document.add(evolutionPara);
        } else {
            document.add(table);
        }
    }

    // ==================== Common Sections ====================

    /**
     * Add defense list section
     */
    private void addDefenseList(Document document, List<Defense> defenses, String title) {
        addSectionTitle(document, title.toUpperCase());

        if (defenses.isEmpty()) {
            Paragraph noData = new Paragraph("Aucune soutenance pour cette période.")
                    .setItalic()
                    .setMarginBottom(20);
            document.add(noData);
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[] { 8, 30, 25, 20, 17 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Header
        table.addHeaderCell(createHeaderCell("N°"));
        table.addHeaderCell(createHeaderCell("Doctorant"));
        table.addHeaderCell(createHeaderCell("Date"));
        table.addHeaderCell(createHeaderCell("Lieu"));
        table.addHeaderCell(createHeaderCell("Mention"));

        // Data rows
        int index = 1;
        for (Defense defense : defenses) {
            UserDTO doctorant = userServiceFacade.getUserById(
                    defense.getDefenseRequest().getDoctorantId());

            table.addCell(createCell(String.valueOf(index++)));
            table.addCell(createCell(doctorant.getFirstName() + " " + doctorant.getLastName()));
            table.addCell(createCell(defense.getDefenseDate() != null
                    ? defense.getDefenseDate().format(DATE_FORMATTER)
                    : "N/A"));
            table.addCell(createCell(defense.getLocation() != null ? defense.getLocation() : "N/A"));
            table.addCell(createCell(defense.getMention() != null
                    ? getMentionAbbreviation(defense.getMention())
                    : "En attente"));
        }

        document.add(table);
    }

    /**
     * Add mention distribution chart
     */
    private void addMentionDistributionChart(Document document, List<Defense> defenses) {
        addSectionTitle(document, "RÉPARTITION DES MENTIONS");

        // Calculate mention distribution
        Map<Mention, Long> mentionCounts = defenses.stream()
                .filter(d -> d.getStatus() == DefenseStatus.COMPLETED)
                .filter(d -> d.getMention() != null)
                .collect(Collectors.groupingBy(Defense::getMention, Collectors.counting()));

        if (mentionCounts.isEmpty()) {
            Paragraph noData = new Paragraph("Aucune mention attribuée pour cette période.")
                    .setItalic()
                    .setMarginBottom(20);
            document.add(noData);
            return;
        }

        Table table = new Table(UnitValue.createPercentArray(new float[] { 50, 15, 35 }))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Header
        table.addHeaderCell(createHeaderCell("Mention"));
        table.addHeaderCell(createHeaderCell("Nombre"));
        table.addHeaderCell(createHeaderCell("Visualisation"));

        // Find max count for scaling
        long maxCount = mentionCounts.values().stream().max(Long::compare).orElse(1L);

        // Data rows
        for (Mention mention : Mention.values()) {
            long count = mentionCounts.getOrDefault(mention, 0L);
            if (count > 0) {
                table.addCell(createCell(getMentionLabel(mention)));
                table.addCell(createCell(String.valueOf(count)));
                table.addCell(createCell(createBarChart(count, maxCount)));
            }
        }

        document.add(table);
    }

    // ==================== Helper Methods ====================

    /**
     * Get defenses for a specific month
     */
    private List<Defense> getDefensesForMonth(YearMonth yearMonth) {
        return defenseRepository.findAll().stream()
                .filter(d -> d.getDefenseDate() != null)
                .filter(d -> {
                    LocalDate defenseDate = d.getDefenseDate().toLocalDate();
                    return defenseDate.getYear() == yearMonth.getYear()
                            && defenseDate.getMonthValue() == yearMonth.getMonthValue();
                })
                .sorted((d1, d2) -> d1.getDefenseDate().compareTo(d2.getDefenseDate()))
                .collect(Collectors.toList());
    }

    /**
     * Get defenses for a specific year
     */
    private List<Defense> getDefensesForYear(int year) {
        return defenseRepository.findAll().stream()
                .filter(d -> d.getDefenseDate() != null)
                .filter(d -> d.getDefenseDate().getYear() == year)
                .sorted((d1, d2) -> d1.getDefenseDate().compareTo(d2.getDefenseDate()))
                .collect(Collectors.toList());
    }

    /**
     * Add section title
     */
    private void addSectionTitle(Document document, String title) {
        Paragraph sectionTitle = new Paragraph(title)
                .setFontSize(13)
                .setBold()
                .setMarginTop(15)
                .setMarginBottom(10)
                .setFontColor(new DeviceRgb(0, 51, 102));
        document.add(sectionTitle);
    }

    /**
     * Add statistic row to table
     */
    private void addStatRow(Table table, String label, String value) {
        table.addCell(createCell(label).setBold());
        table.addCell(createCell(value).setTextAlignment(TextAlignment.RIGHT));
    }

    /**
     * Add comparison row to table
     */
    private void addComparisonRow(Table table, String label, int prevValue, int currValue) {
        table.addCell(createCell(label));
        table.addCell(createCell(String.valueOf(prevValue)).setTextAlignment(TextAlignment.CENTER));
        table.addCell(createCell(String.valueOf(currValue)).setTextAlignment(TextAlignment.CENTER));
    }

    /**
     * Create header cell
     */
    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(10))
                .setBackgroundColor(new DeviceRgb(0, 51, 102))
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(6);
    }

    /**
     * Create regular cell
     */
    private Cell createCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setFontSize(9))
                .setPadding(4)
                .setBorder(Border.NO_BORDER);
    }

    /**
     * Create a simple text-based bar chart
     */
    private String createBarChart(long value, long maxValue) {
        if (maxValue == 0)
            return "";

        int barLength = (int) ((value * 30) / maxValue);
        return "█".repeat(Math.max(0, barLength));
    }

    /**
     * Format YearMonth for display
     */
    private String formatYearMonth(YearMonth yearMonth) {
        String monthName = yearMonth.getMonth()
                .getDisplayName(TextStyle.FULL, FRENCH_LOCALE);
        return capitalize(monthName) + " " + yearMonth.getYear();
    }

    /**
     * Capitalize first letter
     */
    private String capitalize(String text) {
        if (text == null || text.isEmpty())
            return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    /**
     * Get mention label
     */
    private String getMentionLabel(Mention mention) {
        return switch (mention) {
            case TRES_HONORABLE_AVEC_FELICITATIONS -> "Très Honorable avec Félicitations";
            case TRES_HONORABLE -> "Très Honorable";
            case HONORABLE -> "Honorable";
            case PASSABLE -> "Passable";
        };
    }

    /**
     * Get mention abbreviation
     */
    private String getMentionAbbreviation(Mention mention) {
        return switch (mention) {
            case TRES_HONORABLE_AVEC_FELICITATIONS -> "TH+F";
            case TRES_HONORABLE -> "TH";
            case HONORABLE -> "H";
            case PASSABLE -> "P";
        };
    }
}
