package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.dto.external.UserDTO;
import ma.emsi.defenseservice.dto.response.*;
import ma.emsi.defenseservice.entity.*;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.enums.JuryStatus;
import ma.emsi.defenseservice.enums.MemberRole;
import ma.emsi.defenseservice.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing director dashboard
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
@Service
public class DirecteurDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DirecteurDashboardService.class);

    @Autowired
    private DefenseRequestRepository defenseRequestRepository;

    @Autowired
    private DefenseRepository defenseRepository;

    @Autowired
    private JuryRepository juryRepository;

    @Autowired
    private RapportRepository rapportRepository;

    @Autowired
    private UserServiceFacade userServiceFacade;

    /**
     * Get complete dashboard for a director
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
     */
    public DirecteurDashboardDTO getDashboard(Long directeurId) {
        logger.info("📊 Generating dashboard for director ID: {}", directeurId);

        DirecteurDashboardDTO dashboard = new DirecteurDashboardDTO();

        // Enrich director information from User_Service
        UserDTO directeur = userServiceFacade.getUserById(directeurId);
        dashboard.setDirecteur(directeur);

        // Calculate statistics
        dashboard.setStatistiques(calculateStatistics(directeurId));

        // Retrieve defense requests for director's doctorants
        dashboard.setDemandesSoutenance(getDefenseRequestSummaries(directeurId));

        // Retrieve scheduled defenses
        dashboard.setSoutenancesProgrammees(getScheduledDefenses(directeurId));

        // Generate alerts for overdue reports
        dashboard.setAlertes(generateAlerts(directeurId));

        logger.info("✅ Dashboard generated successfully for director ID: {}", directeurId);
        return dashboard;
    }

    /**
     * Calculate statistics for director
     * Requirements: 4.1
     */
    private StatistiquesDirecteurDTO calculateStatistics(Long directeurId) {
        logger.info("📈 Calculating statistics for director ID: {}", directeurId);

        StatistiquesDirecteurDTO stats = new StatistiquesDirecteurDTO();

        // Count active doctorants (excluding COMPLETED and REJECTED)
        List<DefenseRequestStatus> excludedStatuses = Arrays.asList(
                DefenseRequestStatus.COMPLETED,
                DefenseRequestStatus.REJECTED);
        long activeDoctorants = defenseRequestRepository.countActiveDoctorantsByDirector(
                directeurId,
                excludedStatuses);
        stats.setDoctorantsActifs((int) activeDoctorants);

        // Count defenses to plan (AUTHORIZED but not yet scheduled)
        List<DefenseRequestStatus> toPlanStatuses = Arrays.asList(
                DefenseRequestStatus.AUTHORIZED);
        long defensesToPlan = defenseRequestRepository.countByDirectorIdAndStatusIn(
                directeurId,
                toPlanStatuses);
        stats.setSoutenancesAPlanifier((int) defensesToPlan);

        // Count pending reports
        long pendingReports = rapportRepository.countPendingReportsByDirector(directeurId);
        stats.setRapportsEnAttente((int) pendingReports);

        // Count juries to propose (defense requests without jury or with proposed jury)
        List<Jury> juries = juryRepository.findByDirectorId(directeurId);
        long jurysToPropose = juries.stream()
                .filter(j -> j.getStatus() == JuryStatus.PROPOSED)
                .count();
        stats.setJurysAProposer((int) jurysToPropose);

        logger.info(
                "✅ Statistics calculated: {} active doctorants, {} defenses to plan, {} pending reports, {} juries to propose",
                stats.getDoctorantsActifs(), stats.getSoutenancesAPlanifier(),
                stats.getRapportsEnAttente(), stats.getJurysAProposer());

        return stats;
    }

    /**
     * Get defense request summaries for director's doctorants
     * Requirements: 4.2, 4.5
     */
    private List<DefenseRequestSummaryDTO> getDefenseRequestSummaries(Long directeurId) {
        logger.info("📋 Retrieving defense requests for director ID: {}", directeurId);

        List<DefenseRequest> requests = defenseRequestRepository.findByDirectorId(directeurId);

        return requests.stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Map DefenseRequest to DefenseRequestSummaryDTO
     * Requirements: 4.5
     */
    private DefenseRequestSummaryDTO mapToSummaryDTO(DefenseRequest request) {
        DefenseRequestSummaryDTO dto = new DefenseRequestSummaryDTO();
        dto.setId(request.getId());
        dto.setDoctorantId(request.getDoctorantId());
        dto.setStatus(request.getStatus());
        dto.setSubmissionDate(request.getSubmissionDate());

        // Enrich with user information
        UserDTO doctorant = userServiceFacade.getUserById(request.getDoctorantId());
        dto.setDoctorantName(doctorant.getFirstName() + " " + doctorant.getLastName());

        // Calculate doctorate start date and duration
        // Assuming doctorate start date is stored in prerequisites or can be derived
        // For now, using submission date as approximation
        LocalDate startDate = request.getSubmissionDate().toLocalDate().minusYears(3);
        dto.setDoctorateStartDate(startDate);

        long duration = ChronoUnit.YEARS.between(startDate, LocalDate.now());
        dto.setDurationInYears(duration);

        // Determine next action based on status
        dto.setNextAction(determineNextAction(request));

        return dto;
    }

    /**
     * Determine next required action for a defense request
     * Requirements: 4.5
     */
    private String determineNextAction(DefenseRequest request) {
        switch (request.getStatus()) {
            case DRAFT:
                return "Compléter et soumettre la demande";
            case SUBMITTED:
                return "Validation des prérequis";
            case PREREQUISITES_VALIDATION:
                return "Validation du directeur";
            case DIRECTOR_VALIDATION:
                return "Proposer un jury";
            case JURY_PROPOSED:
                return "Validation du jury par l'admin";
            case JURY_VALIDATED:
                return "Attente des rapports des rapporteurs";
            case REPORTS_PENDING:
                return "Attente des rapports des rapporteurs";
            case REPORTS_RECEIVED:
                return "Autorisation de soutenance";
            case AUTHORIZED:
                return "Planifier la soutenance";
            case SCHEDULED:
                return "Attente de la date de soutenance";
            case COMPLETED:
                return "Soutenance terminée";
            case REJECTED:
                return "Demande rejetée";
            default:
                return "Action inconnue";
        }
    }

    /**
     * Get scheduled defenses for director's doctorants
     * Requirements: 4.3
     */
    private List<DefenseScheduledDTO> getScheduledDefenses(Long directeurId) {
        logger.info("📅 Retrieving scheduled defenses for director ID: {}", directeurId);

        List<Defense> defenses = defenseRepository.findScheduledDefensesByDirector(directeurId);

        return defenses.stream()
                .map(this::mapToScheduledDTO)
                .collect(Collectors.toList());
    }

    /**
     * Map Defense to DefenseScheduledDTO
     * Requirements: 4.3
     */
    private DefenseScheduledDTO mapToScheduledDTO(Defense defense) {
        DefenseScheduledDTO dto = new DefenseScheduledDTO();
        dto.setDefenseId(defense.getId());
        dto.setDefenseRequestId(defense.getDefenseRequest().getId());
        dto.setDoctorantId(defense.getDefenseRequest().getDoctorantId());
        dto.setDefenseDate(defense.getDefenseDate());
        dto.setLocation(defense.getLocation());
        dto.setRoom(defense.getRoom());

        // Enrich with user information
        UserDTO doctorant = userServiceFacade.getUserById(defense.getDefenseRequest().getDoctorantId());
        dto.setDoctorantName(doctorant.getFirstName() + " " + doctorant.getLastName());

        return dto;
    }

    /**
     * Generate alerts for overdue reports and pending actions
     * Requirements: 4.4, 4.6
     */
    private List<AlerteDTO> generateAlerts(Long directeurId) {
        logger.info("🚨 Generating alerts for director ID: {}", directeurId);

        List<AlerteDTO> alerts = new ArrayList<>();

        // Get all juries for this director
        List<Jury> juries = juryRepository.findByDirectorId(directeurId);

        for (Jury jury : juries) {
            // Check for overdue reports
            if (jury.getStatus() == JuryStatus.REPORTS_PENDING) {
                List<Rapport> rapports = rapportRepository.findByDefenseRequestId(
                        jury.getDefenseRequest().getId());

                // Get rapporteurs from jury members
                List<JuryMember> rapporteurs = jury.getMembers().stream()
                        .filter(m -> m.getRole() == MemberRole.RAPPORTEUR)
                        .collect(Collectors.toList());

                for (JuryMember rapporteur : rapporteurs) {
                    // Check if rapporteur has submitted report
                    boolean hasSubmitted = rapports.stream()
                            .anyMatch(r -> r.getJuryMember().getId().equals(rapporteur.getId()));

                    if (!hasSubmitted) {
                        // Check if overdue (more than 30 days since jury validation)
                        if (jury.getValidationDate() != null) {
                            long daysSinceValidation = ChronoUnit.DAYS.between(
                                    jury.getValidationDate(),
                                    LocalDateTime.now());

                            if (daysSinceValidation > 30) {
                                AlerteDTO alert = new AlerteDTO();
                                alert.setType("RAPPORT_EN_RETARD");
                                alert.setSeverity("WARNING");
                                alert.setRelatedEntityId(rapporteur.getId());
                                alert.setTimestamp(LocalDateTime.now());

                                UserDTO rapporteurUser = userServiceFacade.getUserById(rapporteur.getProfessorId());
                                alert.setMessage(String.format(
                                        "Rapport en retard de %d jours pour le rapporteur %s %s",
                                        daysSinceValidation,
                                        rapporteurUser.getFirstName(),
                                        rapporteurUser.getLastName()));

                                alerts.add(alert);
                            }
                        }
                    }
                }
            }

            // Check for juries awaiting validation
            if (jury.getStatus() == JuryStatus.PROPOSED) {
                long daysSinceProposal = ChronoUnit.DAYS.between(
                        jury.getProposalDate(),
                        LocalDateTime.now());

                if (daysSinceProposal > 7) {
                    AlerteDTO alert = new AlerteDTO();
                    alert.setType("JURY_EN_ATTENTE_VALIDATION");
                    alert.setSeverity("INFO");
                    alert.setRelatedEntityId(jury.getId());
                    alert.setTimestamp(LocalDateTime.now());
                    alert.setMessage(String.format(
                            "Jury proposé il y a %d jours, en attente de validation administrative",
                            daysSinceProposal));
                    alerts.add(alert);
                }
            }
        }

        logger.info("✅ Generated {} alerts for director ID: {}", alerts.size(), directeurId);
        return alerts;
    }
}
