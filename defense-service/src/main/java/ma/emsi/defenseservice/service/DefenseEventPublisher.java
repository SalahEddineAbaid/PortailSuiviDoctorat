package ma.emsi.defenseservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.defenseservice.dto.event.DefenseEventDTO;
import ma.emsi.defenseservice.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefenseEventPublisher {

    private final KafkaTemplate<String, DefenseEventDTO> kafkaTemplate;

    @Value("${defense.kafka.topic}")
    private String topic;

    /**
     * Publishes DEMANDE_SOUTENANCE_SOUMISE event when a defense request is
     * submitted
     * Requirements: 6.1
     */
    public void publishDemandeSubmitted(DefenseRequest defenseRequest) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("defenseRequestId", defenseRequest.getId());
        payload.put("doctorantId", defenseRequest.getDoctorantId());

        // Get directeur ID from jury if available
        if (defenseRequest.getJury() != null) {
            payload.put("directeurId", defenseRequest.getJury().getDirectorId());
        }

        DefenseEventDTO event = DefenseEventDTO.builder()
                .eventType("DEMANDE_SOUTENANCE_SOUMISE")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        publishEvent(event);
    }

    /**
     * Publishes JURY_PROPOSE event when a jury is proposed
     * Requirements: 6.2
     */
    public void publishJuryProposed(Jury jury) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("defenseRequestId", jury.getDefenseRequest().getId());

        // Extract all jury member IDs
        List<Long> juryMemberIds = jury.getMembers().stream()
                .map(JuryMember::getProfessorId)
                .collect(Collectors.toList());
        payload.put("juryMemberIds", juryMemberIds);

        DefenseEventDTO event = DefenseEventDTO.builder()
                .eventType("JURY_PROPOSE")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        publishEvent(event);
    }

    /**
     * Publishes RAPPORT_SOUMIS event when a rapport is submitted
     * Requirements: 6.3
     */
    public void publishRapportSubmitted(Rapport rapport) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("rapportId", rapport.getId());
        payload.put("defenseRequestId", rapport.getDefenseRequest().getId());
        payload.put("rapporteurId", rapport.getJuryMember().getProfessorId());
        payload.put("favorable", rapport.isFavorable());

        DefenseEventDTO event = DefenseEventDTO.builder()
                .eventType("RAPPORT_SOUMIS")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        publishEvent(event);
    }

    /**
     * Publishes SOUTENANCE_AUTORISEE event when a defense is authorized
     * Requirements: 2.7, 6.4
     */
    public void publishDefenseAuthorized(AutorisationSoutenance autorisation) {
        Map<String, Object> payload = new HashMap<>();
        DefenseRequest defenseRequest = autorisation.getDefenseRequest();

        payload.put("defenseRequestId", defenseRequest.getId());
        payload.put("doctorantId", defenseRequest.getDoctorantId());

        // Get directeur ID from jury
        if (defenseRequest.getJury() != null) {
            payload.put("directeurId", defenseRequest.getJury().getDirectorId());
        }

        // Get jury member IDs
        if (defenseRequest.getJury() != null && defenseRequest.getJury().getMembers() != null) {
            List<Long> juryMemberIds = defenseRequest.getJury().getMembers().stream()
                    .map(JuryMember::getProfessorId)
                    .collect(Collectors.toList());
            payload.put("juryMemberIds", juryMemberIds);
        }

        // Add defense date
        payload.put("defenseDate", autorisation.getDateSoutenance());

        DefenseEventDTO event = DefenseEventDTO.builder()
                .eventType("SOUTENANCE_AUTORISEE")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        publishEvent(event);
    }

    /**
     * Publishes SOUTENANCE_REFUSEE event when a defense authorization is refused
     * Requirements: 2.10, 6.5
     */
    public void publishDefenseRefused(AutorisationSoutenance autorisation) {
        Map<String, Object> payload = new HashMap<>();
        DefenseRequest defenseRequest = autorisation.getDefenseRequest();

        payload.put("defenseRequestId", defenseRequest.getId());
        payload.put("doctorantId", defenseRequest.getDoctorantId());
        payload.put("refusalReason", autorisation.getMotifRefus());

        DefenseEventDTO event = DefenseEventDTO.builder()
                .eventType("SOUTENANCE_REFUSEE")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        publishEvent(event);
    }

    /**
     * Publishes SOUTENANCE_FINALISEE event when a defense is finalized
     * Requirements: 3.6, 6.6
     */
    public void publishDefenseFinalized(Defense defense) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("defenseId", defense.getId());
        payload.put("doctorantId", defense.getDefenseRequest().getDoctorantId());
        payload.put("mention", defense.getMention() != null ? defense.getMention().toString() : null);

        DefenseEventDTO event = DefenseEventDTO.builder()
                .eventType("SOUTENANCE_FINALISEE")
                .timestamp(LocalDateTime.now())
                .payload(payload)
                .build();

        publishEvent(event);
    }

    /**
     * Internal method to publish events to Kafka
     * Requirements: 6.7
     */
    private void publishEvent(DefenseEventDTO event) {
        try {
            kafkaTemplate.send(topic, event.getEventType(), event);
            log.info("Published event {} to topic {}", event.getEventType(), topic);
        } catch (Exception e) {
            log.error("Failed to publish event {} to topic {}: {}",
                    event.getEventType(), topic, e.getMessage(), e);
            // In production, you might want to implement retry logic or dead letter queue
        }
    }
}
