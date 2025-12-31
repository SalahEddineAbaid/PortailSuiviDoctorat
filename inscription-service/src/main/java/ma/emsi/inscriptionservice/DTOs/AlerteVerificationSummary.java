package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO pour le résumé de la vérification des alertes en batch
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlerteVerificationSummary {
    
    /**
     * Nombre total d'inscriptions vérifiées
     */
    private int totalInscriptionsVerifiees;
    
    /**
     * Nombre total d'alertes générées
     */
    private int totalAlertesGenerees;
    
    /**
     * Répartition des alertes par type
     * Clé: Type d'alerte (APPROCHE_3_ANS, APPROCHE_6_ANS, DEPASSE_6_ANS)
     * Valeur: Nombre d'alertes de ce type
     */
    private Map<String, Integer> alertesParType;
    
    /**
     * Nombre d'inscriptions bloquées suite à dépassement de 6 ans
     */
    private int inscriptionsBloqueees;
    
    /**
     * Date et heure de la vérification
     */
    private LocalDateTime dateVerification;
    
    /**
     * Durée du traitement en millisecondes
     */
    private long dureeTraitementMs;
    
    /**
     * Message de statut
     */
    private String message;
}
