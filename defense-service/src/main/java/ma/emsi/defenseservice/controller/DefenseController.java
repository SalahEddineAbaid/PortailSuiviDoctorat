package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.DefenseScheduleDTO;
import ma.emsi.defenseservice.dto.request.FinalizationDTO;
import ma.emsi.defenseservice.dto.response.DefenseResponseDTO;
import ma.emsi.defenseservice.entity.Defense;
import ma.emsi.defenseservice.enums.DefenseStatus;
import ma.emsi.defenseservice.mapper.DefenseMapper;
import ma.emsi.defenseservice.service.DefenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for defense management
 * Implements Requirements 3.1, 3.2, 3.7, 3.8
 */
@RestController
@RequestMapping("/api/defense-service/defenses")
public class DefenseController {

    @Autowired
    private DefenseService defenseService;

    @Autowired
    private DefenseMapper mapper;

    /**
     * Planifier une soutenance avec validation
     */
    @PostMapping
    public ResponseEntity<DefenseResponseDTO> scheduleDefense(
            @Valid @RequestBody DefenseScheduleDTO dto) {
        Defense entity = mapper.toEntity(dto);
        Defense created = defenseService.schedule(entity);
        DefenseResponseDTO response = mapper.toDTO(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer la soutenance d'une demande
     */
    @GetMapping("/defense-request/{requestId}")
    public ResponseEntity<DefenseResponseDTO> getByDefenseRequest(@PathVariable Long requestId) {
        Defense defense = defenseService.getByDefenseRequest(requestId);
        DefenseResponseDTO response = mapper.toDTO(defense);
        return ResponseEntity.ok(response);
    }

    /**
     * Mettre à jour le statut de la soutenance
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<DefenseResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam DefenseStatus status) {
        Defense updated = defenseService.updateStatus(id, status);
        DefenseResponseDTO response = mapper.toDTO(updated);
        return ResponseEntity.ok(response);
    }

    /**
     * Finalize a defense with outcome data and generate procès-verbal
     * Implements Requirements 3.1, 3.2
     * 
     * @param id              Defense ID
     * @param finalizationDTO Finalization data (mention, publication
     *                        recommendation, comments, deliberation date)
     * @return Finalized defense with procès-verbal URL
     */
    @PostMapping("/{id}/finaliser")
    public ResponseEntity<DefenseResponseDTO> finalizeDefense(
            @PathVariable Long id,
            @Valid @RequestBody FinalizationDTO finalizationDTO) {
        Defense finalized = defenseService.finalizeDefense(id, finalizationDTO);
        DefenseResponseDTO response = mapper.toDTO(finalized);
        return ResponseEntity.ok(response);
    }

    /**
     * Download procès-verbal PDF with access control
     * Implements Requirements 3.7, 3.8
     * 
     * Access is restricted to:
     * - Jury members
     * - The doctorant
     * - The directeur
     * - Administrators
     * 
     * @param id     Defense ID
     * @param userId User requesting the PV (from authentication context)
     * @return PDF file as byte array
     */
    @GetMapping("/{id}/proces-verbal")
    public ResponseEntity<byte[]> getProcesVerbal(
            @PathVariable Long id,
            @RequestParam Long userId) {
        byte[] pdfBytes = defenseService.getProcesVerbal(id, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "proces-verbal-defense-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
