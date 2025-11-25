package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.DefenseRequestCreateDTO;
import ma.emsi.defenseservice.dto.response.DefenseRequestResponseDTO;
import ma.emsi.defenseservice.entity.DefenseRequest;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;
import ma.emsi.defenseservice.mapper.DefenseRequestMapper;
import ma.emsi.defenseservice.service.DefenseRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/defense-service/defense-requests")
public class DefenseRequestController {

    @Autowired
    private DefenseRequestService defenseRequestService;

    @Autowired
    private DefenseRequestMapper mapper;

    /**
     * Créer une demande avec validation
     */
    @PostMapping
    public ResponseEntity<DefenseRequestResponseDTO> create(
            @Valid @RequestBody DefenseRequestCreateDTO dto) {
        DefenseRequest entity = mapper.toEntity(dto);
        DefenseRequest created = defenseRequestService.create(entity, dto.getPrerequisitesId());
        DefenseRequestResponseDTO response = mapper.toDTO(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer une demande par ID (optimisé)
     */
    @GetMapping("/{id}")
    public ResponseEntity<DefenseRequestResponseDTO> getById(@PathVariable Long id) {
        DefenseRequest entity = defenseRequestService.getById(id);
        DefenseRequestResponseDTO response = mapper.toDTO(entity);
        return ResponseEntity.ok(response);
    }

    /**
     * Lister toutes les demandes (optimisé)
     */
    @GetMapping
    public ResponseEntity<List<DefenseRequestResponseDTO>> getAll() {
        List<DefenseRequest> entities = defenseRequestService.getAll();
        List<DefenseRequestResponseDTO> responses = entities.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Mettre à jour le statut
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<DefenseRequestResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam DefenseRequestStatus status) {
        DefenseRequest updated = defenseRequestService.updateStatus(id, status);
        DefenseRequestResponseDTO response = mapper.toDTO(updated);
        return ResponseEntity.ok(response);
    }

    /**
     * Supprimer une demande
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        defenseRequestService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
