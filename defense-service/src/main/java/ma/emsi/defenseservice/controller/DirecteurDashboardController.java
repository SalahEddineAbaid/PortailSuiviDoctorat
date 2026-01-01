package ma.emsi.defenseservice.controller;

import ma.emsi.defenseservice.dto.response.DirecteurDashboardDTO;
import ma.emsi.defenseservice.service.DirecteurDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Director Dashboard
 * Provides consolidated view for thesis directors to monitor their doctoral
 * students
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
@RestController
@RequestMapping("/api/defense-service/directeur")
public class DirecteurDashboardController {

    @Autowired
    private DirecteurDashboardService directeurDashboardService;

    /**
     * Get complete dashboard for a director
     * Requirement 4.1: Return statistics including active doctorants count,
     * defenses to plan, pending reports, and juries to propose
     * Requirement 4.2: Return all defense requests for director's doctorants with
     * current status and next action
     * Requirement 4.3: Return all scheduled defenses for director's doctorants with
     * date and location
     * Requirement 4.4: Return alerts for overdue reports or pending actions
     * Requirement 4.5: Include doctorant name, request status, doctorate start
     * date, duration, and next required action
     * 
     * @param directeurId Director ID
     * @return Complete dashboard with statistics, defense requests, scheduled
     *         defenses, and alerts
     */
    @GetMapping("/{directeurId}/dashboard")
    public ResponseEntity<DirecteurDashboardDTO> getDashboard(@PathVariable Long directeurId) {
        DirecteurDashboardDTO dashboard = directeurDashboardService.getDashboard(directeurId);
        return ResponseEntity.ok(dashboard);
    }
}
