package ma.emsi.notificationservice.enums;

/**
 * Enum representing all notification types in the doctoral management system.
 * Each type corresponds to a specific event in the inscription or defense workflow.
 */
public enum TypeNotification {
    // Inscription workflow notifications
    INSCRIPTION_SOUMISE_DIRECTEUR,
    INSCRIPTION_VALIDEE_DIRECTEUR_DOCTORANT,
    INSCRIPTION_VALIDEE_DIRECTEUR_ADMIN,
    INSCRIPTION_REJETEE_DIRECTEUR,
    INSCRIPTION_VALIDEE_ADMIN,
    INSCRIPTION_REJETEE_ADMIN,
    DEROGATION_DEMANDEE,
    
    // Defense workflow notifications
    DEMANDE_SOUTENANCE_SOUMISE_DIRECTEUR,
    JURY_PROPOSE_ADMIN,
    JURY_MEMBRE_INVITE,
    JURY_MEMBRE_ACCEPTE_DIRECTEUR,
    JURY_MEMBRE_DECLINE_DIRECTEUR,
    RAPPORT_SOUMIS_DIRECTEUR,
    AUTORISATION_SOUTENANCE_DOCTORANT,
    SOUTENANCE_PLANIFIEE_TOUS,
    
    // Additional notification types for system events
    RAPPEL_ECHEANCE,
    DOCUMENT_MANQUANT,
    MODIFICATION_CAMPAGNE,
    ALERTE_SYSTEME,
    CONFIRMATION_INSCRIPTION,
    NOTIFICATION_GENERALE
}
