package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.DefenseScheduleDTO;
import ma.emsi.defenseservice.dto.response.DefenseResponseDTO;
import ma.emsi.defenseservice.entity.Defense;
import ma.emsi.defenseservice.enums.DefenseStatus;
import ma.emsi.defenseservice.mapper.DefenseMapper;
import ma.emsi.defenseservice.service.DefenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
