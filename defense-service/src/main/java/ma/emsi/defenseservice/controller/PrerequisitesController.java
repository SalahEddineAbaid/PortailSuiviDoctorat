package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.PrerequisitesCreateDTO;
import ma.emsi.defenseservice.dto.response.PrerequisitesResponseDTO;
import ma.emsi.defenseservice.entity.Prerequisites;
import ma.emsi.defenseservice.mapper.PrerequisitesMapper;
import ma.emsi.defenseservice.service.PrerequisitesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/defense-service/prerequisites")
public class PrerequisitesController {

    @Autowired
    private PrerequisitesService prerequisitesService;

    @Autowired
    private PrerequisitesMapper mapper;

    /**
     * Enregistrer les prérequis avec validation
     */
    @PostMapping
    public ResponseEntity<PrerequisitesResponseDTO> save(
            @Valid @RequestBody PrerequisitesCreateDTO dto) {
        Prerequisites entity = mapper.toEntity(dto);
        Prerequisites saved = prerequisitesService.save(entity);
        PrerequisitesResponseDTO response = mapper.toDTO(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer les prérequis par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PrerequisitesResponseDTO> getById(@PathVariable Long id) {
        Prerequisites entity = prerequisitesService.getById(id);
        PrerequisitesResponseDTO response = mapper.toDTO(entity);
        return ResponseEntity.ok(response);
    }

    /**
     * Valider ou rejeter les prérequis
     */
    @PatchMapping("/{id}/validate")
    public ResponseEntity<PrerequisitesResponseDTO> validate(
            @PathVariable Long id,
            @RequestParam boolean valid) {
        Prerequisites validated = prerequisitesService.validate(id, valid);
        PrerequisitesResponseDTO response = mapper.toDTO(validated);
        return ResponseEntity.ok(response);
    }
}
