package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.JuryCreateDTO;
import ma.emsi.defenseservice.dto.response.JuryResponseDTO;
import ma.emsi.defenseservice.entity.Jury;
import ma.emsi.defenseservice.enums.JuryStatus;
import ma.emsi.defenseservice.mapper.JuryMapper;
import ma.emsi.defenseservice.service.JuryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/defense-service/juries")
public class JuryController {

    @Autowired
    private JuryService juryService;

    @Autowired
    private JuryMapper mapper;

    /**
     * Créer/Proposer un jury avec validation
     */
    @PostMapping
    public ResponseEntity<JuryResponseDTO> createJury(
            @Valid @RequestBody JuryCreateDTO dto) {
        Jury entity = mapper.toEntity(dto);
        Jury created = juryService.create(entity);
        JuryResponseDTO response = mapper.toDTO(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer le jury d'une demande (avec membres)
     */
    @GetMapping("/defense-request/{defenseRequestId}")
    public ResponseEntity<JuryResponseDTO> getByDefenseRequest(@PathVariable Long defenseRequestId) {
        Jury jury = juryService.getByDefenseRequest(defenseRequestId);
        JuryResponseDTO response = mapper.toDTO(jury);
        return ResponseEntity.ok(response);
    }

    /**
     * Mettre à jour le statut du jury
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<JuryResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam JuryStatus status) {
        Jury updated = juryService.updateStatus(id, status);
        JuryResponseDTO response = mapper.toDTO(updated);
        return ResponseEntity.ok(response);
    }
}
