package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.AutorisationDTO;
import ma.emsi.defenseservice.dto.request.RefusDTO;
import ma.emsi.defenseservice.dto.response.AutorisationSoutenanceDTO;
import ma.emsi.defenseservice.dto.response.VerificationResultDTO;
import ma.emsi.defenseservice.service.AutorisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Authorization Management
 * Handles defense authorization verification, approval, and refusal operations
 * Requirements: 2.1, 2.4, 2.8
 */
@RestController
@RequestMapping("/api/defense-service/autorisation")
public class AutorisationController {

    @Autowired
    private AutorisationService autorisationService;

    /**
     * Verify all prerequisites for authorization
     * Requirement 2.1: Check prerequisites, jury, reports, and documents
     * Requirement 2.11: Return detailed information about which checks failed
     * 
     * @param defenseRequestId Defense request ID to verify
     * @return Verification result with detailed checks and blocages
     */
    @PostMapping("/{defenseRequestId}/verifier")
    public ResponseEntity<VerificationResultDTO> verifierPrerequisAutorisation(
            @PathVariable Long defenseRequestId) {
        VerificationResultDTO result = autorisationService.verifierPrerequisAutorisation(defenseRequestId);
        return ResponseEntity.ok(result);
    }

    /**
     * Authorize a defense
     * Requirement 2.4: Create AutorisationSoutenance entity with status AUTORISE
     * Requirement 2.5: Update DefenseRequest status to AUTHORIZED
     * Requirement 2.6: Create Defense entity with scheduled date, location, and
     * room
     * Requirement 2.12: Record administrator ID and authorization timestamp
     * 
     * @param defenseRequestId Defense request ID to authorize
     * @param dto              Authorization data with scheduling information
     * @return Created authorization with HTTP 201 status
     */
    @PostMapping("/{defenseRequestId}/autoriser")
    public ResponseEntity<AutorisationSoutenanceDTO> autoriser(
            @PathVariable Long defenseRequestId,
            @Valid @RequestBody AutorisationDTO dto) {
        AutorisationSoutenanceDTO autorisation = autorisationService.autoriser(defenseRequestId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(autorisation);
    }

    /**
     * Refuse authorization
     * Requirement 2.8: Create AutorisationSoutenance entity with status REFUSE and
     * record refusal reason
     * Requirement 2.9: Update DefenseRequest status to REJECTED
     * Requirement 2.12: Record administrator ID and authorization timestamp
     * 
     * @param defenseRequestId Defense request ID to refuse
     * @param dto              Refusal data with reason
     * @return Created refusal authorization with HTTP 201 status
     */
    @PostMapping("/{defenseRequestId}/refuser")
    public ResponseEntity<AutorisationSoutenanceDTO> refuser(
            @PathVariable Long defenseRequestId,
            @Valid @RequestBody RefusDTO dto) {
        AutorisationSoutenanceDTO autorisation = autorisationService.refuser(defenseRequestId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(autorisation);
    }

    /**
     * Get authorization details
     * Retrieve the authorization for a specific defense request
     * 
     * @param defenseRequestId Defense request ID
     * @return Authorization details with HTTP 200 status
     */
    @GetMapping("/{defenseRequestId}")
    public ResponseEntity<AutorisationSoutenanceDTO> getAutorisation(
            @PathVariable Long defenseRequestId) {
        AutorisationSoutenanceDTO autorisation = autorisationService.getAutorisation(defenseRequestId);
        return ResponseEntity.ok(autorisation);
    }
}
