package ma.emsi.inscriptionservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.inscriptionservice.DTOs.InscriptionRequest;
import ma.emsi.inscriptionservice.DTOs.ValidationRequest;
import ma.emsi.inscriptionservice.DTOs.InscriptionResponse;
import ma.emsi.inscriptionservice.services.InscriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inscriptions")
@RequiredArgsConstructor
public class InscriptionController {

    private final InscriptionService inscriptionService;

    /**
     * Créer une nouvelle demande d'inscription
     */
    @PostMapping
    @PreAuthorize("hasRole('DOCTORANT')")
    public ResponseEntity<InscriptionResponse> creerInscription(
            @Valid @RequestBody InscriptionRequest request) {
        InscriptionResponse response = inscriptionService.creerInscription(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Soumettre l'inscription pour validation
     */
    @PostMapping("/{id}/soumettre")
    @PreAuthorize("hasRole('DOCTORANT')")
    public ResponseEntity<InscriptionResponse> soumettre(
            @PathVariable Long id,
            @RequestParam Long doctorantId) {
        InscriptionResponse response = inscriptionService.soumettre(id, doctorantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer une inscription par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<InscriptionResponse> getInscription(@PathVariable Long id) {
        InscriptionResponse response = inscriptionService.getInscription(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer les inscriptions d'un doctorant
     */
    @GetMapping("/doctorant/{doctorantId}")
    @PreAuthorize("hasAnyRole('DOCTORANT', 'DIRECTEUR', 'ADMIN')")
    public ResponseEntity<List<InscriptionResponse>> getInscriptionsDoctorant(
            @PathVariable Long doctorantId) {
        List<InscriptionResponse> responses = inscriptionService.getInscriptionsDoctorant(doctorantId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Récupérer les inscriptions en attente pour un directeur
     */
    @GetMapping("/directeur/{directeurId}/en-attente")
    @PreAuthorize("hasRole('DIRECTEUR')")
    public ResponseEntity<List<InscriptionResponse>> getInscriptionsEnAttenteDirecteur(
            @PathVariable Long directeurId) {
        List<InscriptionResponse> responses = inscriptionService.getInscriptionsEnAttenteDirecteur(directeurId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Valider l'inscription par le directeur
     */
    @PostMapping("/{id}/valider-directeur")
    @PreAuthorize("hasRole('DIRECTEUR')")
    public ResponseEntity<InscriptionResponse> validerParDirecteur(
            @PathVariable Long id,
            @Valid @RequestBody ValidationRequest request,
            @RequestParam Long directeurId) {
        InscriptionResponse response = inscriptionService.validerParDirecteur(id, request, directeurId);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer les inscriptions en attente admin
     */
    @GetMapping("/admin/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InscriptionResponse>> getInscriptionsEnAttenteAdmin() {
        List<InscriptionResponse> responses = inscriptionService.getInscriptionsEnAttenteAdmin();
        return ResponseEntity.ok(responses);
    }

    /**
     * Valider l'inscription par l'administration
     */
    @PostMapping("/{id}/valider-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InscriptionResponse> validerParAdmin(
            @PathVariable Long id,
            @Valid @RequestBody ValidationRequest request) {
        InscriptionResponse response = inscriptionService.validerParAdmin(id, request);
        return ResponseEntity.ok(response);
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
