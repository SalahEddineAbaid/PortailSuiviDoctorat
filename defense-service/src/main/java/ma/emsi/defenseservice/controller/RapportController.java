package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.RapportSubmitDTO;
import ma.emsi.defenseservice.dto.response.RapportResponseDTO;
import ma.emsi.defenseservice.entity.Rapport;
import ma.emsi.defenseservice.mapper.RapportMapper;
import ma.emsi.defenseservice.service.RapportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/defense-service/rapports")
public class RapportController {

    @Autowired
    private RapportService rapportService;

    @Autowired
    private RapportMapper mapper;

    /**
     * Soumettre un rapport d'évaluation avec validation
     */
    @PostMapping
    public ResponseEntity<RapportResponseDTO> submitRapport(
            @Valid @RequestBody RapportSubmitDTO dto) {
        Rapport entity = mapper.toEntity(dto);
        Rapport submitted = rapportService.submit(entity);
        RapportResponseDTO response = mapper.toDTO(submitted);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer tous les rapports d'une demande (optimisé avec infos enrichies)
     */
    @GetMapping("/defense-request/{defenseRequestId}")
    public ResponseEntity<List<RapportResponseDTO>> getByDefenseRequest(@PathVariable Long defenseRequestId) {
        List<Rapport> rapports = rapportService.getByDefenseRequest(defenseRequestId);
        List<RapportResponseDTO> responses = rapports.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
