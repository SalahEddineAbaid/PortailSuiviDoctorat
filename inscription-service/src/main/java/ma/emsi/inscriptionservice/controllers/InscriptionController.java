package ma.emsi.inscriptionservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.inscriptionservice.DTOs.InscriptionRequest;
import ma.emsi.inscriptionservice.DTOs.ValidationRequest;
import ma.emsi.inscriptionservice.DTOs.InscriptionResponse;
import ma.emsi.inscriptionservice.DTOs.DerogationRequestDTO;
import ma.emsi.inscriptionservice.DTOs.DerogationValidationDTO;
import ma.emsi.inscriptionservice.DTOs.DerogationResponse;
import ma.emsi.inscriptionservice.entities.DerogationRequest;
import ma.emsi.inscriptionservice.services.InscriptionService;
import ma.emsi.inscriptionservice.services.DerogationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/inscriptions")
@RequiredArgsConstructor
@Slf4j
public class InscriptionController {

    private final InscriptionService inscriptionService;
    private final DerogationService derogationService;

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
     * Télécharger l'attestation d'inscription
     */
    @GetMapping("/{id}/attestation")
    @PreAuthorize("hasAnyRole('DOCTORANT', 'DIRECTEUR', 'ADMIN')")
    public ResponseEntity<?> downloadAttestation(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam String role) {
        try {
            byte[] attestationPdf = inscriptionService.getAttestation(id, userId, role);
            
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=attestation_" + id + ".pdf")
                    .body(attestationPdf);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur lors de la récupération de l'attestation"));
        }
    }

    // ==================== DEROGATION ENDPOINTS ====================

    /**
     * Créer une demande de dérogation pour une inscription dépassant 3 ans
     */
    @PostMapping("/{id}/derogation")
    @PreAuthorize("hasRole('DOCTORANT')")
    public ResponseEntity<DerogationResponse> creerDerogation(
            @PathVariable Long id,
            @Valid @RequestBody DerogationRequestDTO request,
            @RequestParam(required = false) MultipartFile documents) {
        try {
            byte[] documentsBytes = null;
            if (documents != null && !documents.isEmpty()) {
                documentsBytes = documents.getBytes();
            }

            DerogationRequest derogation = derogationService.creerDerogation(
                    id, 
                    request.getMotif(), 
                    documentsBytes
            );

            DerogationResponse response = mapToDerogationResponse(derogation);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de la dérogation: " + e.getMessage());
        }
    }

    /**
     * Valider ou rejeter une demande de dérogation par le directeur
     */
    @PostMapping("/{id}/derogation/valider-directeur")
    @PreAuthorize("hasRole('DIRECTEUR')")
    public ResponseEntity<DerogationResponse> validerDerogationParDirecteur(
            @PathVariable Long id,
            @Valid @RequestBody DerogationValidationDTO request,
            @RequestParam Long directeurId) {
        
        // Get the derogation for this inscription
        Optional<DerogationRequest> derogationOpt = derogationService.getDerogation(id);
        if (derogationOpt.isEmpty()) {
            throw new RuntimeException("Aucune demande de dérogation trouvée pour cette inscription");
        }

        DerogationRequest derogation = derogationService.validerParDirecteur(
                derogationOpt.get().getId(),
                request.getApprouve(),
                request.getCommentaire()
        );

        DerogationResponse response = mapToDerogationResponse(derogation);
        return ResponseEntity.ok(response);
    }

    /**
     * Valider ou rejeter une demande de dérogation par le PED
     */
    @PostMapping("/{id}/derogation/valider-ped")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DerogationResponse> validerDerogationParPED(
            @PathVariable Long id,
            @Valid @RequestBody DerogationValidationDTO request) {
        
        // Get the derogation for this inscription
        Optional<DerogationRequest> derogationOpt = derogationService.getDerogation(id);
        if (derogationOpt.isEmpty()) {
            throw new RuntimeException("Aucune demande de dérogation trouvée pour cette inscription");
        }

        DerogationRequest derogation = derogationService.validerParPED(
                derogationOpt.get().getId(),
                request.getApprouve(),
                request.getCommentaire()
        );

        DerogationResponse response = mapToDerogationResponse(derogation);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer la demande de dérogation pour une inscription
     */
    @GetMapping("/{id}/derogation")
    @PreAuthorize("hasAnyRole('DOCTORANT', 'DIRECTEUR', 'ADMIN')")
    public ResponseEntity<DerogationResponse> getDerogation(@PathVariable Long id) {
        Optional<DerogationRequest> derogationOpt = derogationService.getDerogation(id);
        
        if (derogationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DerogationResponse response = mapToDerogationResponse(derogationOpt.get());
        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to map DerogationRequest entity to DerogationResponse DTO
     */
    private DerogationResponse mapToDerogationResponse(DerogationRequest derogation) {
        return DerogationResponse.builder()
                .id(derogation.getId())
                .inscriptionId(derogation.getInscription().getId())
                .motif(derogation.getMotif())
                .statut(derogation.getStatut())
                .dateDemande(derogation.getDateDemande())
                .validateurId(derogation.getValidateurId())
                .commentaireValidation(derogation.getCommentaireValidation())
                .dateValidation(derogation.getDateValidation())
                .hasDocuments(derogation.getDocumentsJustificatifs() != null && 
                             derogation.getDocumentsJustificatifs().length > 0)
                .build();
    }

    // ==================== DASHBOARD ENDPOINT ====================

    /**
     * Récupérer le tableau de bord complet d'un doctorant
     * Accessible au doctorant lui-même, à son directeur, ou à un administrateur
     * 
     * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
     */
    @GetMapping("/doctorant/{id}/dashboard")
    @PreAuthorize("hasAnyRole('DOCTORANT', 'DIRECTEUR', 'ADMIN')")
    public ResponseEntity<ma.emsi.inscriptionservice.DTOs.DashboardResponse> getDashboardDoctorant(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam String role) {
        
        log.info("Fetching dashboard for doctorant: {} by user: {} with role: {}", id, userId, role);
        
        try {
            // Authorization check: verify the user has access to this dashboard
            // - DOCTORANT can only access their own dashboard
            // - DIRECTEUR can access their students' dashboards
            // - ADMIN can access any dashboard
            
            if ("DOCTORANT".equals(role) && !id.equals(userId)) {
                log.warn("Unauthorized access attempt: doctorant {} trying to access dashboard of {}", userId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(null);
            }
            
            if ("DIRECTEUR".equals(role)) {
                // Verify the director is actually the thesis director for this student
                // This is checked by verifying if there's an inscription with this director
                boolean isDirector = inscriptionService.isDirecteurOfDoctorant(userId, id);
                if (!isDirector) {
                    log.warn("Unauthorized access attempt: directeur {} trying to access dashboard of non-student {}", userId, id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(null);
                }
            }
            
            // Fetch the dashboard data
            ma.emsi.inscriptionservice.DTOs.DashboardResponse dashboard = 
                    inscriptionService.getDashboardDoctorant(id);
            
            return ResponseEntity.ok(dashboard);
            
        } catch (RuntimeException e) {
            log.error("Error fetching dashboard for doctorant {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            log.error("Unexpected error fetching dashboard for doctorant {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // ==================== ALERT VERIFICATION ENDPOINT ====================

    /**
     * Vérifier les alertes de durée pour toutes les inscriptions actives (batch processing)
     * Cet endpoint est destiné à être appelé par un service batch ou un administrateur
     * 
     * Requirements: 4.1, 4.2, 4.3
     */
    @GetMapping("/verifier-alertes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ma.emsi.inscriptionservice.DTOs.AlerteVerificationSummary> verifierAlertes() {
        log.info("Démarrage de la vérification des alertes en batch");
        
        try {
            // Récupérer toutes les inscriptions actives (non rejetées et non brouillon)
            List<ma.emsi.inscriptionservice.enums.StatutInscription> statutsActifs = List.of(
                    ma.emsi.inscriptionservice.enums.StatutInscription.EN_ATTENTE_DIRECTEUR,
                    ma.emsi.inscriptionservice.enums.StatutInscription.EN_ATTENTE_ADMIN,
                    ma.emsi.inscriptionservice.enums.StatutInscription.VALIDE
            );
            
            List<ma.emsi.inscriptionservice.entities.Inscription> inscriptionsActives = 
                    inscriptionService.getInscriptionsByStatuts(statutsActifs);
            
            log.info("Nombre d'inscriptions actives à vérifier: {}", inscriptionsActives.size());
            
            // Effectuer la vérification en batch
            ma.emsi.inscriptionservice.DTOs.AlerteVerificationSummary summary = 
                    inscriptionService.verifierAlertesEnBatch(inscriptionsActives);
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("Erreur lors de la vérification des alertes en batch: {}", e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ma.emsi.inscriptionservice.DTOs.AlerteVerificationSummary.builder()
                            .totalInscriptionsVerifiees(0)
                            .totalAlertesGenerees(0)
                            .alertesParType(Map.of())
                            .inscriptionsBloqueees(0)
                            .dateVerification(java.time.LocalDateTime.now())
                            .dureeTraitementMs(0)
                            .message("Erreur lors de la vérification: " + e.getMessage())
                            .build());
        }
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
