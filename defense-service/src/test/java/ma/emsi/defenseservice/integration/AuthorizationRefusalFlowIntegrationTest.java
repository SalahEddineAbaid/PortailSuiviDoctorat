package ma.emsi.defenseservice.integration;

import ma.emsi.defenseservice.dto.event.DefenseEventDTO;
import ma.emsi.defenseservice.dto.request.RefusDTO;
import ma.emsi.defenseservice.dto.response.AutorisationSoutenanceDTO;
import ma.emsi.defenseservice.dto.response.VerificationResultDTO;
import ma.emsi.defenseservice.entity.AutorisationSoutenance;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.entity.Prerequisites;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.enums.StatutAutorisation;
import ma.emsi.defenseservice.repository.AutorisationSoutenanceRepository;
import ma.emsi.defenseservice.repository.DefenseRequestRepository;
import ma.emsi.defenseservice.service.AutorisationService;
import ma.emsi.defenseservice.service.DefenseRequestService;
import ma.emsi.defenseservice.service.PrerequisitesService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.ContainerTestUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for authorization refusal flow
 * Tests: Submit request → Verify prerequisites fail → Refuse authorization
 * Validates: Requirements 2.8, 2.9, 2.10
 */
class AuthorizationRefusalFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DefenseRequestService defenseRequestService;

    @Autowired
    private PrerequisitesService prerequisitesService;

    @Autowired
    private AutorisationService autorisationService;

    @Autowired
    private DefenseRequestRepository defenseRequestRepository;

    @Autowired
    private AutorisationSoutenanceRepository autorisationRepository;

    private BlockingQueue<ConsumerRecord<String, DefenseEventDTO>> kafkaRecords;
    private KafkaMessageListenerContainer<String, DefenseEventDTO> container;

    @Test
    void testAuthorizationRefusalFlow_InsufficientPrerequisites() throws Exception {
        // Setup Kafka consumer
        setupKafkaConsumer();

        // Test data
        Long doctorantId = 1L;
        Long directeurId = 2L;
        Long adminId = 3L;

        // Setup WireMock stubs
        stubUserValidation(doctorantId, "ROLE_DOCTORANT", true);
        stubUserValidation(directeurId, "ROLE_DIRECTEUR", true);

        // ========== STEP 1: Create prerequisites with insufficient publications
        // ==========
        Prerequisites prerequisites = new Prerequisites();
        prerequisites.setDoctorantId(doctorantId);
        prerequisites.setDoctorateStartDate(LocalDate.now().minusYears(3));
        prerequisites.setConferences(3);
        prerequisites.setTrainingHours(250);
        prerequisites.setValid(false);
        prerequisites = prerequisitesService.save(prerequisites);

        // Validate prerequisites (but no Q1/Q2 publications)
        prerequisites = prerequisitesService.validate(prerequisites.getId(), false);
        assertThat(prerequisites.isValid()).isFalse();

        // ========== STEP 2: Submit defense request ==========
        DefenseRequest defenseRequest = new DefenseRequest();
        defenseRequest.setDoctorantId(doctorantId);
        defenseRequest.setPrerequisites(prerequisites);
        defenseRequest = defenseRequestService.create(defenseRequest, prerequisites.getId());

        assertThat(defenseRequest.getStatus()).isEqualTo(DefenseRequestStatus.SUBMITTED);

        // Consume DEMANDE_SOUTENANCE_SOUMISE event
        ConsumerRecord<String, DefenseEventDTO> event1 = kafkaRecords.poll(10, TimeUnit.SECONDS);
        assertThat(event1).isNotNull();
        assertThat(event1.value().getEventType()).isEqualTo("DEMANDE_SOUTENANCE_SOUMISE");

        // ========== STEP 3: Verify prerequisites fail ==========
        VerificationResultDTO verification = autorisationService.verifierPrerequisAutorisation(defenseRequest.getId());

        // Should fail because no Q1/Q2 publications
        assertThat(verification.isPeutAutoriser()).isFalse();
        assertThat(verification.getVerificationsDetaillees()).containsEntry("prerequis_valides", false);
        assertThat(verification.getBlocages()).isNotEmpty();
        assertThat(verification.getBlocages()).anyMatch(b -> b.contains("Prerequisites"));

        // ========== STEP 4: Refuse authorization ==========
        RefusDTO refusDTO = new RefusDTO();
        refusDTO.setAdministrateurId(adminId);
        refusDTO.setMotifRefus("Insufficient Q1/Q2 publications. Minimum 2 required.");
        refusDTO.setCommentaireAdmin("Please add more high-quality publications before resubmitting.");

        AutorisationSoutenanceDTO autorisation = autorisationService.refuser(defenseRequest.getId(), refusDTO);

        // Verify refusal state
        assertThat(autorisation.getStatut()).isEqualTo(StatutAutorisation.REFUSE);
        assertThat(autorisation.getMotifRefus()).isEqualTo("Insufficient Q1/Q2 publications. Minimum 2 required.");
        assertThat(autorisation.getAdministrateurId()).isEqualTo(adminId);
        assertThat(autorisation.getDateAutorisation()).isNotNull();

        // ========== STEP 5: Verify defense request status updated ==========
        DefenseRequest updatedRequest = defenseRequestRepository.findById(defenseRequest.getId()).orElseThrow();
        assertThat(updatedRequest.getStatus()).isEqualTo(DefenseRequestStatus.REJECTED);
        assertThat(updatedRequest.getRejectionReason())
                .isEqualTo("Insufficient Q1/Q2 publications. Minimum 2 required.");

        // ========== STEP 6: Verify authorization entity persisted ==========
        AutorisationSoutenance savedAutorisation = autorisationRepository.findByDefenseRequestId(defenseRequest.getId())
                .orElseThrow();
        assertThat(savedAutorisation.getStatut()).isEqualTo(StatutAutorisation.REFUSE);
        assertThat(savedAutorisation.getMotifRefus()).isNotNull();
        assertThat(savedAutorisation.getAdministrateurId()).isEqualTo(adminId);
        assertThat(savedAutorisation.getDateAutorisation()).isNotNull();
        assertThat(savedAutorisation.getPrerequisValides()).isFalse();

        // ========== STEP 7: Verify SOUTENANCE_REFUSEE event published ==========
        ConsumerRecord<String, DefenseEventDTO> event2 = kafkaRecords.poll(10, TimeUnit.SECONDS);
        assertThat(event2).isNotNull();
        assertThat(event2.value().getEventType()).isEqualTo("SOUTENANCE_REFUSEE");

        Map<String, Object> payload = event2.value().getPayload();
        assertThat(payload).containsKey("defenseRequestId");
        assertThat(payload).containsKey("doctorantId");
        assertThat(payload).containsKey("motifRefus");
        assertThat(payload.get("motifRefus")).isEqualTo("Insufficient Q1/Q2 publications. Minimum 2 required.");

        // Cleanup
        container.stop();
    }

    @Test
    void testAuthorizationRefusalFlow_IncompleteJury() throws Exception {
        // Setup Kafka consumer
        setupKafkaConsumer();

        // Test data
        Long doctorantId = 10L;
        Long directeurId = 20L;
        Long adminId = 30L;

        // Setup WireMock stubs
        stubUserValidation(doctorantId, "ROLE_DOCTORANT", true);
        stubUserValidation(directeurId, "ROLE_DIRECTEUR", true);

        // ========== STEP 1: Create valid prerequisites ==========
        Prerequisites prerequisites = new Prerequisites();
        prerequisites.setDoctorantId(doctorantId);
        prerequisites.setDoctorateStartDate(LocalDate.now().minusYears(3));
        prerequisites.setConferences(3);
        prerequisites.setTrainingHours(250);
        prerequisites.setValid(true);
        prerequisites = prerequisitesService.save(prerequisites);

        // ========== STEP 2: Submit defense request ==========
        DefenseRequest defenseRequest = new DefenseRequest();
        defenseRequest.setDoctorantId(doctorantId);
        defenseRequest.setPrerequisites(prerequisites);
        defenseRequest = defenseRequestService.create(defenseRequest, prerequisites.getId());

        // Consume DEMANDE_SOUTENANCE_SOUMISE event
        kafkaRecords.poll(10, TimeUnit.SECONDS);

        // ========== STEP 3: Verify prerequisites fail (no jury) ==========
        VerificationResultDTO verification = autorisationService.verifierPrerequisAutorisation(defenseRequest.getId());

        // Should fail because no jury
        assertThat(verification.isPeutAutoriser()).isFalse();
        assertThat(verification.getVerificationsDetaillees()).containsEntry("jury_complet", false);

        // ========== STEP 4: Refuse authorization ==========
        RefusDTO refusDTO = new RefusDTO();
        refusDTO.setAdministrateurId(adminId);
        refusDTO.setMotifRefus("Jury composition is incomplete. Please propose a complete jury.");
        refusDTO.setCommentaireAdmin("Jury must include president, rapporteurs, and examiners.");

        AutorisationSoutenanceDTO autorisation = autorisationService.refuser(defenseRequest.getId(), refusDTO);

        // Verify refusal
        assertThat(autorisation.getStatut()).isEqualTo(StatutAutorisation.REFUSE);
        assertThat(autorisation.getMotifRefus()).contains("Jury composition is incomplete");

        // Verify event
        ConsumerRecord<String, DefenseEventDTO> event = kafkaRecords.poll(10, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.value().getEventType()).isEqualTo("SOUTENANCE_REFUSEE");

        // Cleanup
        container.stop();
    }

    private void setupKafkaConsumer() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-refusal");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DefenseEventDTO.class.getName());

        DefaultKafkaConsumerFactory<String, DefenseEventDTO> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("defense-events");
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        kafkaRecords = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, DefenseEventDTO>) kafkaRecords::add);

        container.start();
        ContainerTestUtils.waitForAssignment(container, 1);
    }
}
