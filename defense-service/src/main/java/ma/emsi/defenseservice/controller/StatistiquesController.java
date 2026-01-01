package ma.emsi.defenseservice.controller;

import ma.emsi.defenseservice.dto.response.StatistiquesDTO;
import ma.emsi.defenseservice.service.ReportPdfGenerator;
import ma.emsi.defenseservice.service.StatistiquesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * REST Controller for Statistics and Reporting
 * Provides comprehensive statistics and generates PDF reports about defenses
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7
 */
@RestController
@RequestMapping("/api/defense-service/admin")
public class StatistiquesController {

    @Autowired
    private StatistiquesService statistiquesService;

    @Autowired
    private ReportPdfGenerator reportPdfGenerator;

    /**
     * Get comprehensive statistics about defense requests and defenses
     * Requirement 5.1: Return defense request counts grouped by status
     * Requirement 5.2: Return defense counts grouped by month for the current year
     * Requirement 5.3: Return mention distribution counts across all completed
     * defenses
     * Requirement 5.4: Calculate the overall success rate as a percentage
     * Requirement 5.5: Calculate the average duration from doctorate start to
     * defense completion
     * 
     * @return Comprehensive statistics DTO
     */
    @GetMapping("/statistiques")
    public ResponseEntity<StatistiquesDTO> getStatistiques() {
        StatistiquesDTO statistiques = statistiquesService.getStatistiques();
        return ResponseEntity.ok(statistiques);
    }

    /**
     * Generate a monthly report PDF for the specified month
     * Requirement 5.6: Generate a PDF report including defense list, statistics,
     * and visualizations for the specified month
     * 
     * @param mois Month in format YYYY-MM (e.g., "2025-01")
     * @return PDF document as byte array
     */
    @GetMapping("/rapports/mensuel")
    public ResponseEntity<byte[]> generateMonthlyReport(@RequestParam String mois) {
        // Parse the month parameter (format: YYYY-MM)
        YearMonth yearMonth = YearMonth.parse(mois, DateTimeFormatter.ofPattern("yyyy-MM"));

        byte[] pdfContent = reportPdfGenerator.generateMonthlyReport(yearMonth);

        // Set response headers for PDF download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "rapport-mensuel-" + mois + ".pdf");
        headers.setContentLength(pdfContent.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }

    /**
     * Generate an annual report PDF for the specified year
     * Requirement 5.7: Generate a PDF report including yearly evolution,
     * comparisons, and comprehensive analysis
     * 
     * @param annee Year (e.g., 2025)
     * @return PDF document as byte array
     */
    @GetMapping("/rapports/annuel")
    public ResponseEntity<byte[]> generateAnnualReport(@RequestParam int annee) {
        byte[] pdfContent = reportPdfGenerator.generateAnnualReport(annee);

        // Set response headers for PDF download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "rapport-annuel-" + annee + ".pdf");
        headers.setContentLength(pdfContent.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }
}
