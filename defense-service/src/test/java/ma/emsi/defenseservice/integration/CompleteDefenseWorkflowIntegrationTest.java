package ma.emsi.defenseservice.integration;

import ma.emsi.defenseservice.dto.event.DefenseEventDTO;
import ma.emsi.defenseservice.dto.request.*;
import ma.emsi.defenseservice.dto.response.*;
import ma.emsi.defenseservice.entity.*;
import ma.emsi.defenseservice.enums.*;
import ma.emsi.defenseservice.repository.*;
import ma.emsi.defenseservice.service.*;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for complete defense workflow
 * Tests: Submit request → Validate prerequisites → Propose jury → Submit
 * reports → Authorize → Finalize → Generate PV
 * Validates: All requirements
 */
class CompleteDefenseWorkflowIntegrationTest extends IntegrationTestBase {

        @Autowired
        private DefenseRequestService defenseRequestService;

        @Autowired
        private PrerequisitesService prerequisitesService;

        @Autowired
        private PublicationService publicationService;

        @Autowired
        private JuryService juryService;

        @Autowired
        private RapportService rapportService;

        @Autowired
        private AutorisationService autorisationService;

        @Autowired
        private DefenseService defenseService;

        @Autowired
        private DefenseRequestRepository defenseRequestRepository;

        @Autowired
        private DefenseRepository defenseRepository;

        @Autowired
        private JuryMemberRepository juryMemberRepository;

        @Autowired
        private AutorisationSoutenanceRepository autorisationRepository;

        private BlockingQueue<ConsumerRecord<String, DefenseEventDTO>> kafkaRecords;
        private KafkaMessageListenerContainer<String, DefenseEventDTO> container;

        @Test
        void testCompleteDefenseWorkflow() throws Exception {
                // Setup Kafka consumer to capture events
                setupKafkaConsumer();

                // Test data
                Long doctorantId = 1L;
                Long directeurId = 2L;
                Long adminId = 3L;
                Long professorId1 = 4L;
                Long professorId2 = 5L;
                Long professorId3 = 6L;
                Long professorId4 = 7L;

                // Setup WireMock stubs
                stubUserValidation(doctorantId, "ROLE_DOCTORANT", true);
                stubUserValidation(directeurId, "ROLE_DIRECTEUR", true);
                stubUserValidation(professorId1, "ROLE_PROFESSEUR", true);
                stubUserValidation(professorId2, "ROLE_PROFESSEUR", true);
                stubUserValidation(professorId3, "ROLE_PROFESSEUR", true);
                stubUserValidation(professorId4, "ROLE_PROFESSEUR", true);
                stubUserDetails(doctorantId, "John Doe", "john@test.com");
                stubUserDetails(directeurId, "Dr. Smith", "smith@test.com");
                stubUserDetails(adminId, "Admin User", "admin@test.com");

                // ========== STEP 1: Create and validate prerequisites ==========
                Prerequisites prerequisites = new Prerequisites();
                prerequisites.setDoctorantId(doctorantId);
                prerequisites.setDoctorateStartDate(LocalDate.now().minusYears(3));
                prerequisites.setConferences(3);
                prerequisites.setTrainingHours(250);
                prerequisites.setValid(false);
                prerequisites = prerequisitesService.save(prerequisites);

                // Add Q1/Q2 publications
                PublicationCreateDTO pub1 = new PublicationCreateDTO();
                pub1.setPrerequisitesId(prerequisites.getId());
                pub1.setTitre("Research Paper 1");
                pub1.setJournal("Top Journal");
                pub1.setAnneePublication(2024);
                pub1.setType(TypePublication.JOURNAL_ARTICLE);
                pub1.setQuartile(QuartileJournal.Q1);
                pub1.setDoi("10.1234/test1");
                PublicationResponseDTO savedPub1 = publicationService.createPublication(pub1);

                PublicationCreateDTO pub2 = new PublicationCreateDTO();
                pub2.setPrerequisitesId(prerequisites.getId());
                pub2.setTitre("Research Paper 2");
                pub2.setJournal("Quality Journal");
                pub2.setAnneePublication(2024);
                pub2.setType(TypePublication.JOURNAL_ARTICLE);
                pub2.setQuartile(QuartileJournal.Q2);
                pub2.setDoi("10.1234/test2");
                PublicationResponseDTO savedPub2 = publicationService.createPublication(pub2);

                // Validate publications
                ValidationDTO validation1 = new ValidationDTO();
                validation1.setValidateurId(adminId);
                validation1.setQuartile(QuartileJournal.Q1);
                validation1.setCommentaireValidation("Excellent work");
                publicationService.validatePublication(savedPub1.getId(), validation1);

                ValidationDTO validation2 = new ValidationDTO();
                validation2.setValidateurId(adminId);
                validation2.setQuartile(QuartileJournal.Q2);
                validation2.setCommentaireValidation("Good quality");
                publicationService.validatePublication(savedPub2.getId(), validation2);

                // Validate prerequisites
                prerequisites = prerequisitesService.validate(prerequisites.getId(), true);
                assertThat(prerequisites.isValid()).isTrue();

                // ========== STEP 2: Submit defense request ==========
                DefenseRequest defenseRequest = new DefenseRequest();
                defenseRequest.setDoctorantId(doctorantId);
                defenseRequest.setPrerequisites(prerequisites);
                defenseRequest = defenseRequestService.create(defenseRequest, prerequisites.getId());

                assertThat(defenseRequest.getStatus()).isEqualTo(DefenseRequestStatus.SUBMITTED);

                // Verify DEMANDE_SOUTENANCE_SOUMISE event
                ConsumerRecord<String, DefenseEventDTO> event1 = kafkaRecords.poll(10, TimeUnit.SECONDS);
                assertThat(event1).isNotNull();
                assertThat(event1.value().getEventType()).isEqualTo("DEMANDE_SOUTENANCE_SOUMISE");

                // ========== STEP 3: Propose jury ==========
                JuryCreateDTO juryDTO = new JuryCreateDTO();
                juryDTO.setDefenseRequestId(defenseRequest.getId());
                juryDTO.setDirectorId(directeurId);

                Jury jury = juryService.create(new Jury());
                jury.setDefenseRequest(defenseRequest);
                jury.setDirectorId(directeurId);
                jury.setProposalDate(LocalDateTime.now());
                jury.setStatus(JuryStatus.PROPOSED);

                // Create jury members
                List<JuryMember> members = new ArrayList<>();

                // President
                JuryMember president = new JuryMember();
                president.setJury(jury);
                president.setProfessorId(professorId1);
                president.setRole(MemberRole.PRESIDENT);
                president.setStatus(MemberStatus.ACCEPTED);
                members.add(juryMemberRepository.save(president));

                // Rapporteur 1
                JuryMember rapporteur1 = new JuryMember();
                rapporteur1.setJury(jury);
                rapporteur1.setProfessorId(professorId2);
                rapporteur1.setRole(MemberRole.RAPPORTEUR);
                rapporteur1.setStatus(MemberStatus.ACCEPTED);
                members.add(juryMemberRepository.save(rapporteur1));

                // Rapporteur 2
                JuryMember rapporteur2 = new JuryMember();
                rapporteur2.setJury(jury);
                rapporteur2.setProfessorId(professorId3);
                rapporteur2.setRole(MemberRole.RAPPORTEUR);
                rapporteur2.setStatus(MemberStatus.ACCEPTED);
                members.add(juryMemberRepository.save(rapporteur2));

                // Examiner
                JuryMember examiner = new JuryMember();
                examiner.setJury(jury);
                examiner.setProfessorId(professorId4);
                examiner.setRole(MemberRole.EXAMINATEUR);
                examiner.setStatus(MemberStatus.ACCEPTED);
                members.add(juryMemberRepository.save(examiner));

                jury.setMembers(members);

                // Verify JURY_PROPOSE event
                ConsumerRecord<String, DefenseEventDTO> event2 = kafkaRecords.poll(10, TimeUnit.SECONDS);
                assertThat(event2).isNotNull();
                assertThat(event2.value().getEventType()).isEqualTo("JURY_PROPOSE");

                // ========== STEP 4: Submit rapporteur reports ==========
                // Get rapporteurs
                List<JuryMember> rapporteurs = members.stream()
                                .filter(m -> m.getRole() == MemberRole.RAPPORTEUR)
                                .toList();

                // Submit report from rapporteur 1
                Rapport rapport1 = new Rapport();
                rapport1.setDefenseRequest(defenseRequest);
                rapport1.setJuryMember(rapporteurs.get(0));
                rapport1.setReportUrl("/path/to/report1.pdf");
                rapport1.setFavorable(true);
                rapport1.setSubmissionDate(LocalDateTime.now());
                rapportService.submit(rapport1);

                // Verify RAPPORT_SOUMIS event
                ConsumerRecord<String, DefenseEventDTO> event3 = kafkaRecords.poll(10, TimeUnit.SECONDS);
                assertThat(event3).isNotNull();
                assertThat(event3.value().getEventType()).isEqualTo("RAPPORT_SOUMIS");

                // Submit report from rapporteur 2
                Rapport rapport2 = new Rapport();
                rapport2.setDefenseRequest(defenseRequest);
                rapport2.setJuryMember(rapporteurs.get(1));
                rapport2.setReportUrl("/path/to/report2.pdf");
                rapport2.setFavorable(true);
                rapport2.setSubmissionDate(LocalDateTime.now());
                rapportService.submit(rapport2);

                // Verify second RAPPORT_SOUMIS event
                ConsumerRecord<String, DefenseEventDTO> event4 = kafkaRecords.poll(10, TimeUnit.SECONDS);
                assertThat(event4).isNotNull();
                assertThat(event4.value().getEventType()).isEqualTo("RAPPORT_SOUMIS");

                // ========== STEP 5: Upload documents ==========
                Document document = new Document();
                document.setDefenseRequest(defenseRequest);
                document.setType(DocumentType.MANUSCRIPT);
                document.setFileUrl("/path/to/thesis.pdf");
                document.setFileName("thesis.pdf");
                document.setFileType("application/pdf");
                document.setUploadDate(LocalDateTime.now());
                defenseRequest.getDocuments().add(document);
                defenseRequestRepository.save(defenseRequest);

                // ========== STEP 6: Verify prerequisites for authorization ==========
                VerificationResultDTO verification = autorisationService
                                .verifierPrerequisAutorisation(defenseRequest.getId());
                assertThat(verification.isPeutAutoriser()).isTrue();
                assertThat(verification.getVerificationsDetaillees()).containsEntry("prerequis_valides", true);
                assertThat(verification.getVerificationsDetaillees()).containsEntry("jury_complet", true);
                assertThat(verification.getVerificationsDetaillees()).containsEntry("rapports_favorables", true);
                assertThat(verification.getVerificationsDetaillees()).containsEntry("documents_complets", true);

                // ========== STEP 7: Authorize defense ==========
                AutorisationDTO autorisationDTO = new AutorisationDTO();
                autorisationDTO.setAdministrateurId(adminId);
                autorisationDTO.setDateSoutenance(LocalDateTime.now().plusDays(30));
                autorisationDTO.setLieuSoutenance("University Campus");
                autorisationDTO.setSalleSoutenance("Room 101");
                autorisationDTO.setCommentaireAdmin("All requirements met");

                AutorisationSoutenanceDTO autorisation = autorisationService.autoriser(defenseRequest.getId(),
                                autorisationDTO);
                assertThat(autorisation.getStatut()).isEqualTo(StatutAutorisation.AUTORISE);

                // Verify defense request status updated
                DefenseRequest updatedRequest = defenseRequestRepository.findById(defenseRequest.getId()).orElseThrow();
                assertThat(updatedRequest.getStatus()).isEqualTo(DefenseRequestStatus.AUTHORIZED);

                // Verify Defense entity created
                Defense defense = defenseRepository.findByDefenseRequestId(defenseRequest.getId()).orElseThrow();
                assertThat(defense.getStatus()).isEqualTo(DefenseStatus.SCHEDULED);
                assertThat(defense.getLocation()).isEqualTo("University Campus");

                // Verify SOUTENANCE_AUTORISEE event
                ConsumerRecord<String, DefenseEventDTO> event5 = kafkaRecords.poll(10, TimeUnit.SECONDS);
                assertThat(event5).isNotNull();
                assertThat(event5.value().getEventType()).isEqualTo("SOUTENANCE_AUTORISEE");

                // ========== STEP 8: Finalize defense ==========
                FinalizationDTO finalizationDTO = new FinalizationDTO();
                finalizationDTO.setMention(Mention.TRES_HONORABLE);
                finalizationDTO.setPublicationRecommended(true);
                finalizationDTO.setJuryComments("Outstanding defense presentation");
                finalizationDTO.setDeliberationDate(LocalDateTime.now());

                Defense finalizedDefense = defenseService.finalizeDefense(defense.getId(), finalizationDTO);
                assertThat(finalizedDefense.getStatus()).isEqualTo(DefenseStatus.COMPLETED);
                assertThat(finalizedDefense.getMention()).isEqualTo(Mention.TRES_HONORABLE);
                assertThat(finalizedDefense.getProcesVerbalUrl()).isNotNull();

                // Verify SOUTENANCE_FINALISEE event
                ConsumerRecord<String, DefenseEventDTO> event6 = kafkaRecords.poll(10, TimeUnit.SECONDS);
                assertThat(event6).isNotNull();
                assertThat(event6.value().getEventType()).isEqualTo("SOUTENANCE_FINALISEE");

                // ========== STEP 9: Verify procès-verbal generation ==========
                byte[] pvPdf = defenseService.getProcesVerbal(defense.getId(), doctorantId);
                assertThat(pvPdf).isNotEmpty();
                assertThat(pvPdf).startsWith(new byte[] { 0x25, 0x50, 0x44, 0x46 }); // PDF magic number

                // ========== STEP 10: Verify all events published ==========
                assertThat(kafkaRecords).hasSize(0); // All events consumed

                // Cleanup
                container.stop();
        }

        private void setupKafkaConsumer() {
                Map<String, Object> consumerProps = new HashMap<>();
                consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
                consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-complete");
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
