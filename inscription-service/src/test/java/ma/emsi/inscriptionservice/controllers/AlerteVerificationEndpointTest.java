package ma.emsi.inscriptionservice.controllers;

import ma.emsi.inscriptionservice.DTOs.AlerteVerificationSummary;
import ma.emsi.inscriptionservice.enums.StatutInscription;
import ma.emsi.inscriptionservice.services.AlerteService;
import ma.emsi.inscriptionservice.services.InscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration pour l'endpoint de vérification des alertes en batch
 * 
 * Requirements: 4.1, 4.2, 4.3
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlerteVerificationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InscriptionService inscriptionService;

    @MockBean
    private AlerteService alerteService;

    @BeforeEach
    void setUp() {
        // Configuration commune pour les tests
    }

    /**
     * Test: L'endpoint doit être accessible uniquement aux administrateurs
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    void verifierAlertes_sansAuthentification_retourne401() throws Exception {
        mockMvc.perform(get("/api/inscriptions/verifier-alertes"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test: L'endpoint doit être accessible aux administrateurs
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void verifierAlertes_avecRoleAdmin_retourne200() throws Exception {
        // Arrange
        List<StatutInscription> statutsActifs = List.of(
                StatutInscription.EN_ATTENTE_DIRECTEUR,
                StatutInscription.EN_ATTENTE_ADMIN,
                StatutInscription.VALIDE
        );

        Map<String, Integer> alertesParType = new HashMap<>();
        alertesParType.put("APPROCHE_3_ANS", 2);
        alertesParType.put("APPROCHE_6_ANS", 1);
        alertesParType.put("DEPASSE_6_ANS", 0);

        AlerteVerificationSummary summary = AlerteVerificationSummary.builder()
                .totalInscriptionsVerifiees(10)
                .totalAlertesGenerees(3)
                .alertesParType(alertesParType)
                .inscriptionsBloqueees(0)
                .dateVerification(LocalDateTime.now())
                .dureeTraitementMs(150)
                .message("Vérification terminée: 10 inscriptions vérifiées, 3 alertes générées, 0 inscriptions bloquées")
                .build();

        when(inscriptionService.getInscriptionsByStatuts(statutsActifs))
                .thenReturn(List.of());
        when(inscriptionService.verifierAlertesEnBatch(anyList()))
                .thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/api/inscriptions/verifier-alertes"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalInscriptionsVerifiees").value(10))
                .andExpect(jsonPath("$.totalAlertesGenerees").value(3))
                .andExpect(jsonPath("$.inscriptionsBloqueees").value(0))
                .andExpect(jsonPath("$.alertesParType.APPROCHE_3_ANS").value(2))
                .andExpect(jsonPath("$.alertesParType.APPROCHE_6_ANS").value(1))
                .andExpect(jsonPath("$.alertesParType.DEPASSE_6_ANS").value(0));
    }

    /**
     * Test: L'endpoint ne doit pas être accessible aux doctorants
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    @WithMockUser(roles = "DOCTORANT")
    void verifierAlertes_avecRoleDoctorant_retourne403() throws Exception {
        mockMvc.perform(get("/api/inscriptions/verifier-alertes"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test: L'endpoint ne doit pas être accessible aux directeurs
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    @WithMockUser(roles = "DIRECTEUR")
    void verifierAlertes_avecRoleDirecteur_retourne403() throws Exception {
        mockMvc.perform(get("/api/inscriptions/verifier-alertes"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test: L'endpoint doit gérer les erreurs gracieusement
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void verifierAlertes_avecErreur_retourne500AvecMessage() throws Exception {
        // Arrange
        when(inscriptionService.getInscriptionsByStatuts(any()))
                .thenThrow(new RuntimeException("Erreur de base de données"));

        // Act & Assert
        mockMvc.perform(get("/api/inscriptions/verifier-alertes"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.totalInscriptionsVerifiees").value(0))
                .andExpect(jsonPath("$.totalAlertesGenerees").value(0))
                .andExpect(jsonPath("$.message").value("Erreur lors de la vérification: Erreur de base de données"));
    }

    /**
     * Test: L'endpoint doit retourner un résumé vide si aucune inscription active
     * Requirements: 4.1, 4.2, 4.3
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void verifierAlertes_aucuneInscriptionActive_retourneSummaireVide() throws Exception {
        // Arrange
        Map<String, Integer> alertesParType = new HashMap<>();
        alertesParType.put("APPROCHE_3_ANS", 0);
        alertesParType.put("APPROCHE_6_ANS", 0);
        alertesParType.put("DEPASSE_6_ANS", 0);

        AlerteVerificationSummary summary = AlerteVerificationSummary.builder()
                .totalInscriptionsVerifiees(0)
                .totalAlertesGenerees(0)
                .alertesParType(alertesParType)
                .inscriptionsBloqueees(0)
                .dateVerification(LocalDateTime.now())
                .dureeTraitementMs(10)
                .message("Vérification terminée: 0 inscriptions vérifiées, 0 alertes générées, 0 inscriptions bloquées")
                .build();

        when(inscriptionService.getInscriptionsByStatuts(any()))
                .thenReturn(List.of());
        when(inscriptionService.verifierAlertesEnBatch(anyList()))
                .thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/api/inscriptions/verifier-alertes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInscriptionsVerifiees").value(0))
                .andExpect(jsonPath("$.totalAlertesGenerees").value(0));
    }
}
