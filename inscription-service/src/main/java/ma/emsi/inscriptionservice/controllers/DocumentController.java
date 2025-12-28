package ma.emsi.inscriptionservice.controllers;

import lombok.RequiredArgsConstructor;
import ma.emsi.inscriptionservice.DTOs.DocumentResponse;
import ma.emsi.inscriptionservice.enums.TypeDocument;
import ma.emsi.inscriptionservice.services.DocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Téléverser un document
     */
    @PostMapping("/{inscriptionId}/upload")
    @PreAuthorize("hasRole('DOCTORANT')")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable Long inscriptionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("typeDocument") TypeDocument typeDocument) {
        DocumentResponse response = documentService.uploadDocument(inscriptionId, file, typeDocument);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer les documents d'une inscription
     */
    @GetMapping("/{inscriptionId}")
    public ResponseEntity<List<DocumentResponse>> getDocuments(@PathVariable Long inscriptionId) {
        List<DocumentResponse> responses = documentService.getDocuments(inscriptionId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Télécharger un document
     */
    @GetMapping("/download/{documentId}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        Resource resource = documentService.downloadDocument(documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Supprimer un document
     */
    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('DOCTORANT')")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.ok(Map.of("message", "Document supprimé avec succès"));
    }

    /**
     * Gestion des erreurs
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
    }
}
