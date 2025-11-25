package ma.emsi.defenseservice.controller;

import jakarta.validation.Valid;
import ma.emsi.defenseservice.dto.request.DocumentUploadDTO;
import ma.emsi.defenseservice.dto.response.DocumentResponseDTO;
import ma.emsi.defenseservice.entity.Document;
import ma.emsi.defenseservice.mapper.DocumentMapper;
import ma.emsi.defenseservice.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/defense-service/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentMapper mapper;

    /**
     * Uploader un document avec validation
     */
    @PostMapping
    public ResponseEntity<DocumentResponseDTO> uploadDocument(
            @Valid @RequestBody DocumentUploadDTO dto) {
        Document entity = mapper.toEntity(dto);
        Document uploaded = documentService.upload(entity);
        DocumentResponseDTO response = mapper.toDTO(uploaded);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer tous les documents d'une demande (optimisé)
     */
    @GetMapping("/defense-request/{requestId}")
    public ResponseEntity<List<DocumentResponseDTO>> getByDefenseRequest(@PathVariable Long requestId) {
        List<Document> documents = documentService.getByDefenseRequest(requestId);
        List<DocumentResponseDTO> responses = documents.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Supprimer un document
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
