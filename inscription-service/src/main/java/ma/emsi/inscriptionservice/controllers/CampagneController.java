package ma.emsi.inscriptionservice.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.emsi.inscriptionservice.DTOs.CampagneRequest;
import ma.emsi.inscriptionservice.DTOs.CampagneResponse;
import ma.emsi.inscriptionservice.DTOs.CloneCampagneRequest;
import ma.emsi.inscriptionservice.DTOs.StatistiquesCampagne;
import ma.emsi.inscriptionservice.services.CampagneService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campagnes")
@RequiredArgsConstructor
public class CampagneController {

    private final CampagneService campagneService;

    /**
     * Créer une nouvelle campagne
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampagneResponse> creerCampagne(
            @Valid @RequestBody CampagneRequest request) {
        CampagneResponse response = campagneService.creerCampagne(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Récupérer toutes les campagnes
     */
    @GetMapping
    public ResponseEntity<List<CampagneResponse>> getAllCampagnes() {
        List<CampagneResponse> responses = campagneService.getAllCampagnes();
        return ResponseEntity.ok(responses);
    }

    /**
     * Récupérer les campagnes actives
     */
    @GetMapping("/actives")
    public ResponseEntity<List<CampagneResponse>> getCampagnesActives() {
        List<CampagneResponse> responses = campagneService.getCampagnesActives();
        return ResponseEntity.ok(responses);
    }

    /**
     * Récupérer une campagne par ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CampagneResponse> getCampagne(@PathVariable Long id) {
        CampagneResponse response = campagneService.getCampagne(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Fermer une campagne
     */
    @PutMapping("/{id}/fermer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampagneResponse> fermerCampagne(@PathVariable Long id) {
        CampagneResponse response = campagneService.fermerCampagne(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Modifier une campagne
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampagneResponse> modifierCampagne(
            @PathVariable Long id,
            @Valid @RequestBody CampagneRequest request) {
        CampagneResponse response = campagneService.modifierCampagne(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Récupérer les statistiques d'une campagne
     */
    @GetMapping("/{id}/statistiques")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StatistiquesCampagne> getStatistiques(@PathVariable Long id) {
        StatistiquesCampagne statistiques = campagneService.getStatistiques(id);
        return ResponseEntity.ok(statistiques);
    }

    /**
     * Cloner une campagne existante avec de nouvelles dates
     */
    @PostMapping("/{id}/cloner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampagneResponse> clonerCampagne(
            @PathVariable Long id,
            @Valid @RequestBody CloneCampagneRequest request) {
        CampagneResponse response = campagneService.clonerCampagne(
                id, 
                request.getDateDebut(), 
                request.getDateFin()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
