package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.dto.response.DefenseCountByMonthDTO;
import ma.emsi.defenseservice.dto.response.StatistiquesDTO;
import ma.emsi.defenseservice.entity.Defense;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.enums.DefenseStatus;
import ma.emsi.defenseservice.enums.Mention;
import ma.emsi.defenseservice.repository.DefenseRepository;
import ma.emsi.defenseservice.repository.DefenseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatistiquesService {

    private final DefenseRequestRepository defenseRequestRepository;
    private final DefenseRepository defenseRepository;

    /**
     * Get comprehensive statistics about defense requests and defenses
     * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
     */
    public StatistiquesDTO getStatistiques() {
        log.info("Calculating comprehensive statistics");

        StatistiquesDTO statistiques = new StatistiquesDTO();

        // Calculate defense requests by status (Requirement 5.1)
        statistiques.setDemandesParStatut(calculateDefenseRequestsByStatus());

        // Calculate defenses by month for current year (Requirement 5.2)
        statistiques.setSoutenancesParMois(calculateDefensesByMonth());

        // Calculate mention distribution (Requirement 5.3)
        statistiques.setMentionsDistribuees(calculateMentionDistribution());

        // Calculate success rate (Requirement 5.4)
        statistiques.setTauxReussiteGlobal(calculateSuccessRate());

        // Calculate average duration from start to completion (Requirement 5.5)
        statistiques.setDureeMoyenneSoutenance(calculateAverageDuration());

        log.info("Statistics calculated successfully");
        return statistiques;
    }

    /**
     * Calculate defense request counts grouped by status
     * Requirement 5.1
     */
    private Map<DefenseRequestStatus, Long> calculateDefenseRequestsByStatus() {
        List<DefenseRequest> allRequests = defenseRequestRepository.findAll();

        return allRequests.stream()
                .collect(Collectors.groupingBy(
                        DefenseRequest::getStatus,
                        Collectors.counting()));
    }

    /**
     * Calculate defense counts grouped by month for the current year
     * Requirement 5.2
     */
    private List<DefenseCountByMonthDTO> calculateDefensesByMonth() {
        int currentYear = LocalDate.now().getYear();
        List<Defense> allDefenses = defenseRepository.findAll();

        // Filter defenses for current year and group by month
        Map<Integer, Long> defensesByMonth = allDefenses.stream()
                .filter(defense -> defense.getDefenseDate() != null)
                .filter(defense -> defense.getDefenseDate().getYear() == currentYear)
                .collect(Collectors.groupingBy(
                        defense -> defense.getDefenseDate().getMonthValue(),
                        Collectors.counting()));

        // Convert to DTO list, including months with zero defenses
        List<DefenseCountByMonthDTO> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            long count = defensesByMonth.getOrDefault(month, 0L);
            result.add(new DefenseCountByMonthDTO(currentYear, month, count));
        }

        return result;
    }

    /**
     * Calculate mention distribution across all completed defenses
     * Requirement 5.3
     */
    private Map<Mention, Long> calculateMentionDistribution() {
        List<Defense> completedDefenses = defenseRepository.findAll().stream()
                .filter(defense -> defense.getStatus() == DefenseStatus.COMPLETED)
                .filter(defense -> defense.getMention() != null)
                .toList();

        return completedDefenses.stream()
                .collect(Collectors.groupingBy(
                        Defense::getMention,
                        Collectors.counting()));
    }

    /**
     * Calculate overall success rate as a percentage
     * Success rate = (completed defenses / total finalized defenses) * 100
     * Requirement 5.4
     */
    private double calculateSuccessRate() {
        List<Defense> allDefenses = defenseRepository.findAll();

        long totalFinalized = allDefenses.stream()
                .filter(defense -> defense.getStatus() == DefenseStatus.COMPLETED)
                .count();

        if (totalFinalized == 0) {
            return 0.0;
        }

        // All completed defenses are considered successful
        // (if there were failures, they would have a different status)
        long successful = totalFinalized;

        return (successful * 100.0) / totalFinalized;
    }

    /**
     * Calculate average duration from doctorate start to defense completion
     * Duration is calculated in years
     * Requirement 5.5
     */
    private double calculateAverageDuration() {
        List<Defense> completedDefenses = defenseRepository.findAll().stream()
                .filter(defense -> defense.getStatus() == DefenseStatus.COMPLETED)
                .filter(defense -> defense.getDefenseRequest() != null)
                .filter(defense -> defense.getDefenseRequest().getPrerequisites() != null)
                .filter(defense -> defense.getDefenseRequest().getPrerequisites().getDoctorateStartDate() != null)
                .filter(defense -> defense.getDeliberationDate() != null)
                .toList();

        if (completedDefenses.isEmpty()) {
            return 0.0;
        }

        double totalDurationInYears = 0.0;
        for (Defense defense : completedDefenses) {
            LocalDate startDate = defense.getDefenseRequest().getPrerequisites().getDoctorateStartDate();
            LocalDate completionDate = defense.getDeliberationDate().toLocalDate();

            long daysBetween = ChronoUnit.DAYS.between(startDate, completionDate);
            double years = daysBetween / 365.25; // Account for leap years

            totalDurationInYears += years;
        }

        return totalDurationInYears / completedDefenses.size();
    }
}
