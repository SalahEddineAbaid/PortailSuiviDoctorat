package ma.emsi.notificationservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.emsi.notificationservice.dtos.NotificationDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import ma.emsi.notificationservice.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the complete notification flow.
 * Tests end-to-end processing from Kafka consumption to email sending and persistence.
 * 
 * Requirements: 14.2 - Testing the Kafka consumer with EmbeddedKafka
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"notifications", "notifications-dlq"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9093",
        "port=9093"
    }
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.mail.host=localhost",
    "spring.mail.port=3025",
    "notification.email.from=test@test.com",
    "resilience4j.circuitbreaker.instances.emailService.sliding-window-size=10",
    "resilience4j.retry.instances.emailService.max-attempts=2",
    "logging.level.ma.emsi.notificationservice=DEBUG"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class NotificationFlowIntegrationTest {

    @Autowired
    private KafkaTemplate<String, NotificationDTO> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void testCompleteNotificationFlow_Success() {
        // Arrange
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("nomDoctorant", "Ahmed Bennani");
        donnees.put("nomDirecteur", "Dr. Alami");
        donnees.put("dateInscription", "2024-01-15");

        NotificationDTO notificationDTO = NotificationDTO.builder()
            .type(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR)
            .destinataire("directeur@emsi.ma")
            .sujet("Nouvelle inscription soumise")
            .messageTexte("Une nouvelle inscription a été soumise par Ahmed Bennani")
            .priorite(PrioriteNotification.NORMALE)
            .donnees(donnees)
            .build();

        // Act
        kafkaTemplate.send("notifications", notificationDTO);

        // Assert - Wait for async processing
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findAll();
                assertThat(notifications).isNotEmpty();
                
                Notification notification = notifications.get(0);
                assertThat(notification.getType()).isEqualTo(TypeNotification.INSCRIPTION_SOUMISE_DIRECTEUR);
                assertThat(notification.getDestinataire()).isEqualTo("directeur@emsi.ma");
                assertThat(notification.getSujet()).isEqualTo("Nouvelle inscription soumise");
                assertThat(notification.getStatut()).isIn(StatutNotification.SENT, StatutNotification.FAILED);
                assertThat(notification.getMessageTexte()).isNotNull();
                assertThat(notification.getMessageHtml()).isNotNull();
                assertThat(notification.getDateCreation()).isNotNull();
            });
    }

    @Test
    void testNotificationFlow_WithDifferentTypes() {
        // Arrange
        NotificationDTO inscriptionValideeDTO = NotificationDTO.builder()
            .type(TypeNotification.INSCRIPTION_VALIDEE_DIRECTEUR_DOCTORANT)
            .destinataire("doctorant@emsi.ma")
            .sujet("Inscription validée")
            .messageTexte("Votre inscription a été validée")
            .priorite(PrioriteNotification.HAUTE)
            .build();

        NotificationDTO inscriptionRejeteeDTO = NotificationDTO.builder()
            .type(TypeNotification.INSCRIPTION_REJETEE_DIRECTEUR)
            .destinataire("doctorant2@emsi.ma")
            .sujet("Inscription rejetée")
            .messageTexte("Votre inscription a été rejetée")
            .priorite(PrioriteNotification.URGENTE)
            .build();

        // Act
        kafkaTemplate.send("notifications", inscriptionValideeDTO);
        kafkaTemplate.send("notifications", inscriptionRejeteeDTO);

        // Assert
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findAll();
                assertThat(notifications).hasSize(2);
                
                assertThat(notifications)
                    .extracting(Notification::getType)
                    .containsExactlyInAnyOrder(
                        TypeNotification.INSCRIPTION_VALIDEE_DIRECTEUR_DOCTORANT,
                        TypeNotification.INSCRIPTION_REJETEE_DIRECTEUR
                    );
            });
    }

    @Test
    void testNotificationFlow_PersistenceValidation() {
        // Arrange
        Map<String, Object> donnees = new HashMap<>();
        donnees.put("key1", "value1");
        donnees.put("key2", 123);
        donnees.put("key3", true);

        NotificationDTO notificationDTO = NotificationDTO.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("user@emsi.ma")
            .sujet("Test notification")
            .messageTexte("Test message")
            .priorite(PrioriteNotification.NORMALE)
            .donnees(donnees)
            .build();

        // Act
        kafkaTemplate.send("notifications", notificationDTO);

        // Assert - Verify all fields are persisted correctly
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findAll();
                assertThat(notifications).hasSize(1);
                
                Notification notification = notifications.get(0);
                assertThat(notification.getId()).isNotNull();
                assertThat(notification.getType()).isEqualTo(TypeNotification.NOTIFICATION_GENERALE);
                assertThat(notification.getDestinataire()).isEqualTo("user@emsi.ma");
                assertThat(notification.getSujet()).isEqualTo("Test notification");
                assertThat(notification.getMessageTexte()).isNotNull();
                assertThat(notification.getMessageHtml()).isNotNull();
                assertThat(notification.getStatut()).isNotNull();
                assertThat(notification.getPriorite()).isEqualTo(PrioriteNotification.NORMALE);
                assertThat(notification.getDonnees()).isNotNull();
                assertThat(notification.getNombreTentatives()).isGreaterThanOrEqualTo(0);
                assertThat(notification.getDateCreation()).isNotNull();
            });
    }

    @Test
    void testNotificationFlow_WithNullDonnees() {
        // Arrange
        NotificationDTO notificationDTO = NotificationDTO.builder()
            .type(TypeNotification.NOTIFICATION_GENERALE)
            .destinataire("user@emsi.ma")
            .sujet("Test without data")
            .messageTexte("Test message without donnees")
            .priorite(PrioriteNotification.NORMALE)
            .donnees(null)
            .build();

        // Act
        kafkaTemplate.send("notifications", notificationDTO);

        // Assert
        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findAll();
                assertThat(notifications).hasSize(1);
                
                Notification notification = notifications.get(0);
                assertThat(notification.getDestinataire()).isEqualTo("user@emsi.ma");
                assertThat(notification.getStatut()).isIn(StatutNotification.SENT, StatutNotification.FAILED);
            });
    }

    @Test
    void testNotificationFlow_MultipleNotifications() {
        // Arrange - Send multiple notifications
        for (int i = 0; i < 5; i++) {
            NotificationDTO notificationDTO = NotificationDTO.builder()
                .type(TypeNotification.NOTIFICATION_GENERALE)
                .destinataire("user" + i + "@emsi.ma")
                .sujet("Test notification " + i)
                .messageTexte("Test message " + i)
                .priorite(PrioriteNotification.NORMALE)
                .build();
            
            kafkaTemplate.send("notifications", notificationDTO);
        }

        // Assert
        await()
            .atMost(15, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> {
                List<Notification> notifications = notificationRepository.findAll();
                assertThat(notifications).hasSize(5);
                
                assertThat(notifications)
                    .extracting(Notification::getDestinataire)
                    .containsExactlyInAnyOrder(
                        "user0@emsi.ma",
                        "user1@emsi.ma",
                        "user2@emsi.ma",
                        "user3@emsi.ma",
                        "user4@emsi.ma"
                    );
            });
    }
}
