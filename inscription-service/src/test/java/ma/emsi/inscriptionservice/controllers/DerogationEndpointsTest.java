package ma.emsi.inscriptionservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import ma.emsi.inscriptionservice.DTOs.DerogationRequestDTO;
import ma.emsi.inscriptionservice.DTOs.DerogationValidationDTO;
import ma.emsi.inscriptionservice.entities.DerogationRequest;
import ma.emsi.inscriptionservice.enums.StatutDerogation;
import ma.emsi.inscriptionservice.services.DerogationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for derogation REST endpoints.
 * Tests the four main derogation endpoints: create, validate by director, validate by PED, and get.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class DerogationEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DerogationService derogationService;

    @Test
    @WithMockUser(roles = "DOCTORANT")
    public void testCreerDerogation_Success() throws Exception {
        // Arrange
        Long inscriptionId = 1L;
        DerogationRequestDTO requestDTO = DerogationRequestDTO.builder()
                .motif("Je demande une dérogation car j'ai eu des problèmes de santé qui ont retardé mes recherches.")
                .build();

        DerogationRequest mockDerogation = DerogationRequest.builder()
                .id(1L)
                .motif(requestDTO.getMotif())
                .statut(StatutDerogation.EN_ATTENTE)
                .dateDemande(LocalDateTime.now())
                .build();

        when(derogationService.creerDerogation(eq(inscriptionId), anyString(), any()))
                .thenReturn(mockDerogation);

        // Act & Assert
        mockMvc.perform(post("/api/inscriptions/{id}/derogation", inscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.motif").value(requestDTO.getMotif()))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    @WithMockUser(roles = "DIRECTEUR")
    public void testValiderDerogationParDirecteur_Approve() throws Exception {
        // Arrange
        Long inscriptionId = 1L;
        DerogationValidationDTO validationDTO = DerogationValidationDTO.builder()
                .approuve(true)
                .commentaire("J'approuve cette demande de dérogation.")
                .build();

        DerogationRequest existingDerogation = DerogationRequest.builder()
                .id(1L)
                .statut(StatutDerogation.EN_ATTENTE)
                .build();

        DerogationRequest approvedDerogation = DerogationRequest.builder()
                .id(1L)
                .statut(StatutDerogation.APPROUVE_DIRECTEUR)
                .commentaireValidation(validationDTO.getCommentaire())
                .dateValidation(LocalDateTime.now())
                .build();

        when(derogationService.getDerogation(inscriptionId))
                .thenReturn(Optional.of(existingDerogation));
        when(derogationService.validerParDirecteur(eq(1L), eq(true), anyString()))
                .thenReturn(approvedDerogation);

        // Act & Assert
        mockMvc.perform(post("/api/inscriptions/{id}/derogation/valider-directeur", inscriptionId)
                        .param("directeurId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("APPROUVE_DIRECTEUR"))
                .andExpect(jsonPath("$.commentaireValidation").value(validationDTO.getCommentaire()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testValiderDerogationParPED_Approve() throws Exception {
        // Arrange
        Long inscriptionId = 1L;
        DerogationValidationDTO validationDTO = DerogationValidationDTO.builder()
                .approuve(true)
                .commentaire("Le PED approuve cette dérogation.")
                .build();

        DerogationRequest existingDerogation = DerogationRequest.builder()
                .id(1L)
                .statut(StatutDerogation.APPROUVE_DIRECTEUR)
                .build();

        DerogationRequest approvedDerogation = DerogationRequest.builder()
                .id(1L)
                .statut(StatutDerogation.APPROUVE_PED)
                .commentaireValidation(validationDTO.getCommentaire())
                .dateValidation(LocalDateTime.now())
                .build();

        when(derogationService.getDerogation(inscriptionId))
                .thenReturn(Optional.of(existingDerogation));
        when(derogationService.validerParPED(eq(1L), eq(true), anyString()))
                .thenReturn(approvedDerogation);

        // Act & Assert
        mockMvc.perform(post("/api/inscriptions/{id}/derogation/valider-ped", inscriptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("APPROUVE_PED"))
                .andExpect(jsonPath("$.commentaireValidation").value(validationDTO.getCommentaire()));
    }

    @Test
    @WithMockUser(roles = "DOCTORANT")
    public void testGetDerogation_Found() throws Exception {
        // Arrange
        Long inscriptionId = 1L;
        DerogationRequest mockDerogation = DerogationRequest.builder()
                .id(1L)
                .motif("Test motif")
                .statut(StatutDerogation.EN_ATTENTE)
                .dateDemande(LocalDateTime.now())
                .build();

        when(derogationService.getDerogation(inscriptionId))
                .thenReturn(Optional.of(mockDerogation));

        // Act & Assert
        mockMvc.perform(get("/api/inscriptions/{id}/derogation", inscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.motif").value("Test motif"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    @WithMockUser(roles = "DOCTORANT")
    public void testGetDerogation_NotFound() throws Exception {
        // Arrange
        Long inscriptionId = 1L;
        when(derogationService.getDerogation(inscriptionId))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/inscriptions/{id}/derogation", inscriptionId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "DIRECTEUR")
    public void testValiderDerogationParDirecteur_Reject() throws Exception {
        // Arrange
        Long inscriptionId = 1L;
        DerogationValidationDTO validationDTO = DerogationValidationDTO.builder()
                .approuve(false)
                .commentaire("Je rejette cette demande.")
                .build();

        DerogationRequest existingDerogation = DerogationRequest.builder()
                .id(1L)
                .statut(StatutDerogation.EN_ATTENTE)
                .build();

        DerogationRequest rejectedDerogation = DerogationRequest.builder()
                .id(1L)
                .statut(StatutDerogation.REJETE)
                .commentaireValidation(validationDTO.getCommentaire())
                .dateValidation(LocalDateTime.now())
                .build();

        when(derogationService.getDerogation(inscriptionId))
                .thenReturn(Optional.of(existingDerogation));
        when(derogationService.validerParDirecteur(eq(1L), eq(false), anyString()))
                .thenReturn(rejectedDerogation);

        // Act & Assert
        mockMvc.perform(post("/api/inscriptions/{id}/derogation/valider-directeur", inscriptionId)
                        .param("directeurId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validationDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETE"))
                .andExpect(jsonPath("$.commentaireValidation").value(validationDTO.getCommentaire()));
    }
}
