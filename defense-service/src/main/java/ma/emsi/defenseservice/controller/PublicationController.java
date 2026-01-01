package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.PublicationCreateDTO;
import ma.emsi.defenseservice.dto.request.ValidationDTO;
import ma.emsi.defenseservice.dto.response.PublicationResponseDTO;
import ma.emsi.defenseservice.service.PublicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Publication Management
 * Handles publication creation, validation, and retrieval operations
 * Requirements: 1.1, 1.3, 1.5
 */
@RestController
@RequestMapping("/api/defense-service/publications")
public class PublicationController {

    @Autowired
    private PublicationService publicationService;

    /**
     * Create a new publication
     * Requirement 1.1: Store publication with all fields
     * 
     * @param dto Publication creation data with validation
     * @return Created publication with HTTP 201 status
     */
    @PostMapping
    public ResponseEntity<PublicationResponseDTO> createPublication(
            @Valid @RequestBody PublicationCreateDTO dto) {
        PublicationResponseDTO created = publicationService.createPublication(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get all publications for a given prerequisites
     * Requirement 1.1: Retrieve publications by prerequisites
     * 
     * @param id Prerequisites ID
     * @return List of publications with HTTP 200 status
     */
    @GetMapping("/prerequisites/{id}")
    public ResponseEntity<List<PublicationResponseDTO>> getPublicationsByPrerequisites(
            @PathVariable Long id) {
        List<PublicationResponseDTO> publications = publicationService.getPublicationsByPrerequisites(id);
        return ResponseEntity.ok(publications);
    }

    /**
     * Validate a publication
     * Requirement 1.3: Record validator ID, verification date, verified quartile,
     * and validation comments
     * 
     * @param id  Publication ID
     * @param dto Validation data with validator ID, quartile, and comments
     * @return Validated publication with HTTP 200 status
     */
    @PutMapping("/{id}/valider")
    public ResponseEntity<PublicationResponseDTO> validatePublication(
            @PathVariable Long id,
            @Valid @RequestBody ValidationDTO dto) {
        PublicationResponseDTO validated = publicationService.validatePublication(id, dto);
        return ResponseEntity.ok(validated);
    }

    /**
     * Get all publications pending validation
     * Requirement 1.5: Return all publications where validation status is false
     * 
     * @return List of pending publications with HTTP 200 status
     */
    @GetMapping("/en-attente-validation")
    public ResponseEntity<List<PublicationResponseDTO>> getPendingValidations() {
        List<PublicationResponseDTO> pendingPublications = publicationService.getPendingValidations();
        return ResponseEntity.ok(pendingPublications);
    }
}
