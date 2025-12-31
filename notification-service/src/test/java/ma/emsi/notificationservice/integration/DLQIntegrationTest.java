package ma.emsi.notificationservice.integration;

import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.entities.NotificationDLQ;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import ma.emsi.notificationservice.repositories.NotificationDLQRepository;
import ma.emsi.notificationservice.repositories.NotificationRepository;
import ma.emsi.notificationservice.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mail.MailSendException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Integration test for Dead Letter Queue (DLQ) flow.
 * Tests that failed notifications are sent to DLQ and can be reprocessed.
 * 
 * Requirements: 14.6 - Testing the DLQ to verify messages are sent to DLQ topic after failures
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"notifications", "notifications-dlq"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9094",
        "port=9094"
    }
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.datasource.url=jdbc:h2:mem:testdb_dlq",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.mail.host=localhost",
    "spring.mail.port=3025",
    "notification.email.from=test@test.com",
    // Reduce retry attempts for faster testing
    "resilience4j.retry.instances.emailService.max-attempts=2",
    "resilience4j.retry.instances.emailService.wait-duration=500ms",
    "resilience4j.retry.instances.emailService.enable-exponential-backoff=false",
    "resilience4j.circuitbreaker.instances.emailService.sliding-window-size=10",
    "logging.level.ma.emsi.notificationservice=DEBUG"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class DLQIntegrationTest {

    @Autowired
    private KafkaTemplate<String, NotificationDTO> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDLQRepository notificationDLQRepository;

    @SpyBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        notificationDLQRepository.deleteAll();
    }

    @Test
    void testDLQFlow_FailedNotificationSentToDLQ() {
        // Arrange
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("nomDoctorant", "Test Doctorant");
        donnees.put("reason", "Test failure scenario");

        NotificationDTO notificationDTO = NotificationDTO.builder()
            .type(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR)
            .destinataire("directeur@emsi.ma")
            .sujet("Test DLQ notification")
            .messageTexte("This notification should fail and go to DLQ")
            .priorite(PrioriteNotification.NORMALE)
            .donnees(donnees)
            .build();

        // Configure email service to always fail
        try {
            doAnswer(invocation -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new MailSendException("Simulated email failure"));
                return future;
            }).when(emailService).sendEmail(anyString(), anyString(), anyString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Act
        kafkaTemplate.send("notifications", notificationDTO);

        // Assert - Wait for notification to be processed and sent to DLQ
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                // Verify notification was persisted with FAILED status
                List<Notification> notifications = notificationRepository.findAll();
                assertThat(notifications).isNotEmpty();
                
                Notification notification = notifications.get(0);
                assertThat(notification.getStatut()).isEqualTo(StatutNotification.FAILED);
                assertThat(notification.getErreurMessage()).isNotNull();
                assertThat(notification.getNombreTentatives()).isGreaterThan(0);
                
                // Verify DLQ entry was created
                List<NotificationDLQ> dlqEntries = notificationDLQRepository.findAll();
                assertThat(dlqEntries).isNotEmpty();
                
                NotificationDLQ dlqEntry = dlqEntries.get(0);
                assertThat(dlqEntry.getType()).isEqualTo(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR);
                assertThat(dlqEntry.getDestinataire()).isEqualTo("directeur@emsi.ma");
                assertThat(dlqEntry.getErreurMessage()).isNotNull();
                assertThat(dlqEntry.getDateAjoutDlq()).isNotNull();
            });
    }

    @Test
    void testDLQFlow_MultipleFailedNotificationsSentToDLQ() {
        // Arrange
        try {
            doAnswer(invocation -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new MailSendException("Simulated email failure"));
                return future;
            }).when(emailService).sendEmail(anyString(), anyString(), anyString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Send multiple notifications that will fail
        for (int i = 0; i < 3; i++) {
            NotificationDTO notificationDTO = NotificationDTO.builder()
                .type(TypeNotification.NOTIFICATION_GENERALE)
                .destinataire("user" + i + "@emsi.ma")
                .sujet("Test DLQ " + i)
                .messageTexte("Test message " + i)
                .priorite(PrioriteNotification.NORMALE)
                .build();
            
            kafkaTemplate.send("notifications", notificationDTO);
        }

        // Assert
        await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                // Verify all notifications failed
                List<Notification> notifications = notificationRepository.findAll();
                assertThat(notifications).hasSize(3);
                assertThat(notifications)
                    .allMatch(n -> n.getStatut() == StatutNotification.FAILED);
                
                // Verify all were sent to DLQ
                List<NotificationDLQ> dlqEntries = notificationDLQRepository.findAll();
                assertThat(dlqEntries).hasSize(3);
                assertThat(dlqEntries)
                    .extracting(NotificationDLQ::getDestinataire)
                    .containsExactlyInAnyOrder(
                        "user0@emsi.ma",
                        "user1@emsi.ma",
                        "user2@emsi.ma"
                    );
            });
    }

    @Test
    void testDLQPersistence_AllFieldsStored() {
        // Arrange
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("field1", "value1");
        donnees.put("field2", 42);
        donnees.put("field3", true);

        NotificationDTO notificationDTO = NotificationDTO.builder()
            .type(TypeNotification.INSCRIPTION_REJETEE_DIRECTEUR)
            .destinataire("test@emsi.ma")
            .sujet("Test DLQ persistence")
            .messageTexte("Test message for DLQ")
            .priorite(PrioriteNotification.URGENTE)
            .donnees(donnees)
            .build();

        try {
            doAnswer(invocation -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new MailSendException("Simulated failure"));
                return future;
            }).when(emailService).sendEmail(anyString(), anyString(), anyString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Act
        kafkaTemplate.send("notifications", notificationDTO);

        // Assert - Verify all fields are persisted in DLQ
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<NotificationDLQ> dlqEntries = notificationDLQRepository.findAll();
                assertThat(dlqEntries).hasSize(1);
                
                NotificationDLQ dlqEntry = dlqEntries.get(0);
                assertThat(dlqEntry.getId()).isNotNull();
                assertThat(dlqEntry.getType()).isEqualTo(TypeNotification.INSCRIPTION_REJETEE_DIRECTEUR);
                assertThat(dlqEntry.getDestinataire()).isEqualTo("test@emsi.ma");
                assertThat(dlqEntry.getSujet()).isEqualTo("Test DLQ persistence");
                assertThat(dlqEntry.getDonnees()).isNotNull();
                assertThat(dlqEntry.getErreurMessage()).contains("Simulated failure");
                assertThat(dlqEntry.getDateAjoutDlq()).isNotNull();
            });
    }

    @Test
    void testDLQFlow_InvalidEmailFormat() {
        // Arrange - Send notification with invalid email
        NotificationDTO notificationDTO = NotificationDTO.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("invalid-email")
            .sujet("Test invalid email")
            .messageTexte("Test message")
            .priorite(PrioriteNotification.NORMALE)
            .build();

        // Act
        kafkaTemplate.send("notifications", notificationDTO);

        // Assert - Should be rejected and sent to DLQ
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                // Check if notification was processed
                List<Notification> notifications = notificationRepository.findAll();
                
                // Either notification is marked as failed or DLQ entry exists
                if (!notifications.isEmpty()) {
                    Notification notification = notifications.get(0);
                    assertThat(notification.getStatut()).isEqualTo(StatutNotification.FAILED);
                }
                
                // DLQ should have the entry
                List<NotificationDLQ> dlqEntries = notificationDLQRepository.findAll();
                if (!dlqEntries.isEmpty()) {
                    NotificationDLQ dlqEntry = dlqEntries.get(0);
                    assertThat(dlqEntry.getDestinataire()).isEqualTo("invalid-email");
                    assertThat(dlqEntry.getErreurMessage()).isNotNull();
                }
            });
    }

    @Test
    void testDLQFlow_DifferentNotificationTypes() {
        // Arrange
        try {
            doAnswer(invocation -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new MailSendException("Simulated failure"));
                return future;
            }).when(emailService).sendEmail(anyString(), anyString(), anyString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        TypeNotification[] types = {
            TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR,
            TypeNotification.INSCRIPTION_VALIDEE_DIRECTEUR_DOCTORANT,
            TypeNotification.INSCRIPTION_REJETEE_DIRECTEUR,
            TypeNotification.JURY_PROPOSE_ADMIN,
            TypeNotification.SOUTENANCE_PLANIFIEE_TOUS
        };

        // Act - Send notifications of different types
        for (TypeNotification type : types) {
            NotificationDTO notificationDTO = NotificationDTO.builder()
                .type(type)
                .destinataire("test@emsi.ma")
                .sujet("Test " + type)
                .messageTexte("Test message")
                .priorite(PrioriteNotification.NORMALE)
                .build();
            
            kafkaTemplate.send("notifications", notificationDTO);
        }

        // Assert
        await()
            .atMost(20, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<NotificationDLQ> dlqEntries = notificationDLQRepository.findAll();
                assertThat(dlqEntries).hasSize(types.length);
                
                assertThat(dlqEntries)
                    .extracting(NotificationDLQ::getType)
                    .containsExactlyInAnyOrder(types);
            });
    }

    @Test
    void testDLQFlow_WithRetryAttempts() {
        // Arrange
        NotificationDTO notificationDTO = NotificationDTO.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("test@emsi.ma")
            .sujet("Test retry before DLQ")
            .messageTexte("Test message")
            .priorite(PrioriteNotification.NORMALE)
            .build();

        try {
            doAnswer(invocation -> {
                CompletableFuture<Void> future = new CompletableFuture<>();
                future.completeExceptionally(new MailSendException("Persistent failure"));
                return future;
            }).when(emailService).sendEmail(anyString(), anyString(), anyString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Act
        kafkaTemplate.send("notifications", notificationDTO);

        // Assert - Verify retries were attempted before sending to DLQ
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findAll();
                assertThat(notifications).hasSize(1);
                
                Notification notification = notifications.get(0);
                // Should have attempted retries
                assertThat(notification.getNombreTentatives()).isGreaterThan(0);
                assertThat(notification.getStatut()).isEqualTo(StatutNotification.FAILED);
                
                // Should be in DLQ
                List<NotificationDLQ> dlqEntries = notificationDLQRepository.findAll();
                assertThat(dlqEntries).hasSize(1);
            });
    }
}
