package ma.emsi.defenseservice.integration;

import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.entity.Prerequisites;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.service.DefenseRequestService;
import ma.emsi.defenseservice.service.PrerequisitesService;
import ma.emsi.defenseservice.service.UserServiceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Integration test for resilience patterns
 * Tests: User_Service failures, circuit breaker, retry, and fallback behavior
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5
 */
class ResiliencePatternsIntegrationTest extends IntegrationTestBase {

        @Autowired
        private DefenseRequestService defenseRequestService;

        @Autowired
        private PrerequisitesService prerequisitesService;

        @Autowired
        private UserServiceFacade userServiceFacade;

        @Test
        void testRetryPolicyExecution() {
                // Test data
                Long doctorantId = 100L;
                Long directeurId = 200L;

                // Setup WireMock to fail twice then succeed (tests retry)
                stubFor(get(urlEqualTo("/api/users/" + doctorantId + "/validate-role?role=ROLE_DOCTORANT"))
                                .inScenario("Retry Scenario")
                                .whenScenarioStateIs("Started")
                                .willReturn(aResponse()
                                                .withStatus(500)
                                                .withBody("Service Unavailable"))
                                .willSetStateTo("First Retry"));

                stubFor(get(urlEqualTo("/api/users/" + doctorantId + "/validate-role?role=ROLE_DOCTORANT"))
                                .inScenario("Retry Scenario")
                                .whenScenarioStateIs("First Retry")
                                .willReturn(aResponse()
                                                .withStatus(500)
                                                .withBody("Service Unavailable"))
                                .willSetStateTo("Second Retry"));

                stubFor(get(urlEqualTo("/api/users/" + doctorantId + "/validate-role?role=ROLE_DOCTORANT"))
                                .inScenario("Retry Scenario")
                                .whenScenarioStateIs("Second Retry")
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("true")));

                // Create prerequisites
                Prerequisites prerequisites = new Prerequisites();
                prerequisites.setDoctorantId(doctorantId);
                prerequisites.setDoctorateStartDate(LocalDate.now().minusYears(3));
                prerequisites.setConferences(3);
                prerequisites.setTrainingHours(250);
                prerequisites.setValid(true);
                prerequisites = prerequisitesService.save(prerequisites);

                // Create defense request - should succeed after retries
                DefenseRequest defenseRequest = new DefenseRequest();
                defenseRequest.setDoctorantId(doctorantId);
                defenseRequest.setPrerequisites(prerequisites);

                DefenseRequest savedRequest = defenseRequestService.create(defenseRequest, prerequisites.getId());

                // Verify request was created successfully after retries
                assertThat(savedRequest).isNotNull();
                assertThat(savedRequest.getStatus()).isEqualTo(DefenseRequestStatus.SUBMITTED);

                // Verify that the service was called 3 times (2 failures + 1 success)
                verify(exactly(3),
                                getRequestedFor(urlEqualTo(
                                                "/api/users/" + doctorantId + "/validate-role?role=ROLE_DOCTORANT")));
        }

        @Test
        void testCircuitBreakerActivation() {
                // Test data
                Long doctorantId = 101L;
                Long directeurId = 201L;

                // Setup WireMock to always fail (triggers circuit breaker)
                stubFor(get(urlMatching("/api/users/" + doctorantId + "/validate-role.*"))
                                .willReturn(aResponse()
                                                .withStatus(500)
                                                .withBody("Service Unavailable")));

                stubUserValidation(directeurId, "ROLE_DIRECTEUR", true);

                // Create prerequisites
                Prerequisites prerequisites = new Prerequisites();
                prerequisites.setDoctorantId(doctorantId);
                prerequisites.setDoctorateStartDate(LocalDate.now().minusYears(3));
                prerequisites.setConferences(3);
                prerequisites.setTrainingHours(250);
                prerequisites.setValid(true);

                // First few calls should fail and trigger circuit breaker
                assertThatThrownBy(() -> prerequisitesService.save(prerequisites))
                                .isInstanceOf(IllegalArgumentException.class);

                // Make multiple calls to open the circuit breaker
                for (int i = 0; i < 5; i++) {
                        try {
                                Prerequisites newPrereq = new Prerequisites();
                                newPrereq.setDoctorantId(doctorantId);
                                newPrereq.setDoctorateStartDate(LocalDate.now().minusYears(3));
                                newPrereq.setConferences(3);
                                newPrereq.setTrainingHours(250);
                                prerequisitesService.save(newPrereq);
                        } catch (Exception e) {
                                // Expected to fail
                        }
                }

                // Wait for circuit breaker to open
                await().atMost(Duration.ofSeconds(5))
                                .pollInterval(Duration.ofMillis(500))
                                .untilAsserted(() -> {
                                        // Circuit breaker should be open now
                                        // Subsequent calls should fail fast without hitting the service
                                        assertThatThrownBy(() -> {
                                                Prerequisites newPrereq = new Prerequisites();
                                                newPrereq.setDoctorantId(doctorantId);
                                                newPrereq.setDoctorateStartDate(LocalDate.now().minusYears(3));
                                                newPrereq.setConferences(3);
                                                newPrereq.setTrainingHours(250);
                                                prerequisitesService.save(newPrereq);
                                        }).isInstanceOf(Exception.class);
                                });

                // Verify that the service was called multiple times before circuit opened
                verify(moreThanOrExactly(3),
                                getRequestedFor(urlMatching("/api/users/" + doctorantId + "/validate-role.*")));
        }

        @Test
        void testGracefulDegradation() {
                // Test data
                Long doctorantId = 102L;
                Long directeurId = 202L;

                // Setup WireMock: User validation fails but we should get fallback
                stubFor(get(urlMatching("/api/users/" + doctorantId + "/validate-role.*"))
                                .willReturn(aResponse()
                                                .withStatus(500)
                                                .withBody("Service Unavailable")));

                stubFor(get(urlMatching("/api/users/" + doctorantId + "/details"))
                                .willReturn(aResponse()
                                                .withStatus(500)
                                                .withBody("Service Unavailable")));

                stubUserValidation(directeurId, "ROLE_DIRECTEUR", true);

                // Create prerequisites - should fail but gracefully
                Prerequisites prerequisites = new Prerequisites();
                prerequisites.setDoctorantId(doctorantId);
                prerequisites.setDoctorateStartDate(LocalDate.now().minusYears(3));
                prerequisites.setConferences(3);
                prerequisites.setTrainingHours(250);
                prerequisites.setValid(true);

                // This should throw an exception because validation is critical
                assertThatThrownBy(() -> prerequisitesService.save(prerequisites))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("n'existe pas ou n'a pas le rôle DOCTORANT");

                // Verify fallback behavior - service should have attempted the call
                verify(moreThanOrExactly(1),
                                getRequestedFor(urlMatching("/api/users/" + doctorantId + "/validate-role.*")));
        }

        @Test
        void testRetryWithEventualSuccess() {
                // Test data
                Long doctorantId = 103L;
                Long directeurId = 203L;

                // Setup WireMock: Fail first, then succeed
                stubFor(get(urlEqualTo("/api/users/" + doctorantId + "/validate-role?role=ROLE_DOCTORANT"))
                                .inScenario("Eventual Success")
                                .whenScenarioStateIs("Started")
                                .willReturn(aResponse()
                                                .withStatus(503)
                                                .withBody("Temporarily Unavailable"))
                                .willSetStateTo("Ready"));

                stubFor(get(urlEqualTo("/api/users/" + doctorantId + "/validate-role?role=ROLE_DOCTORANT"))
                                .inScenario("Eventual Success")
                                .whenScenarioStateIs("Ready")
                                .willReturn(aResponse()
                                                .withStatus(200)
                                                .withHeader("Content-Type", "application/json")
                                                .withBody("true")));

                stubUserValidation(directeurId, "ROLE_DIRECTEUR", true);

                // Create prerequisites - should succeed after retry
                Prerequisites prerequisites = new Prerequisites();
                prerequisites.setDoctorantId(doctorantId);
                prerequisites.setDoctorateStartDate(LocalDate.now().minusYears(3));
                prerequisites.setConferences(3);
                prerequisites.setTrainingHours(250);
                prerequisites.setValid(true);

                Prerequisites saved = prerequisitesService.save(prerequisites);

                // Verify success
                assertThat(saved).isNotNull();
                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getDoctorantId()).isEqualTo(doctorantId);

                // Verify retry happened
                verify(moreThanOrExactly(2),
                                getRequestedFor(urlEqualTo(
                                                "/api/users/" + doctorantId + "/validate-role?role=ROLE_DOCTORANT")));
        }
}
