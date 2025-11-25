package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.JuryMemberCreateDTO;
import ma.emsi.defenseservice.dto.response.JuryMemberResponseDTO;
import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.enums.MemberStatus;
import ma.emsi.defenseservice.mapper.JuryMemberMapper;
import ma.emsi.defenseservice.service.JuryMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/defense-service/jury-members")
public class JuryMemberController {

    @Autowired
    private JuryMemberService juryMemberService;

    @Autowired
    private JuryMemberMapper mapper;

    /**
     * Ajouter un membre au jury avec validation
     */
    @PostMapping
    public ResponseEntity<JuryMemberResponseDTO> addMember(
            @Valid @RequestBody JuryMemberCreateDTO dto) {
        JuryMember entity = mapper.toEntity(dto);
        JuryMember added = juryMemberService.add(entity);
        JuryMemberResponseDTO response = mapper.toDTO(added);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer tous les membres d'un jury (optimisé)
     */
    @GetMapping("/jury/{juryId}")
    public ResponseEntity<List<JuryMemberResponseDTO>> getByJury(@PathVariable Long juryId) {
        List<JuryMember> members = juryMemberService.getByJury(juryId);
        List<JuryMemberResponseDTO> responses = members.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Mettre à jour le statut d'un membre (acceptation/refus)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<JuryMemberResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam MemberStatus status) {
        JuryMember updated = juryMemberService.updateStatus(id, status);
        JuryMemberResponseDTO response = mapper.toDTO(updated);
        return ResponseEntity.ok(response);
    }
}
