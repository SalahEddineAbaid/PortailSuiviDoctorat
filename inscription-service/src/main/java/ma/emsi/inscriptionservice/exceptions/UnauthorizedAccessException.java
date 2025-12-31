package ma.emsi.inscriptionservice.exceptions;

/**
 * Exception thrown when a user attempts to access a resource they are not authorized to access.
 */
public class UnauthorizedAccessException extends RuntimeException {
    
    private final Long userId;
    private final String resource;
    private final String action;
    
    public UnauthorizedAccessException(String message) {
        super(message);
        this.userId = null;
        this.resource = null;
        this.action = null;
    }
    
    public UnauthorizedAccessException(Long userId, String resource, String action) {
        super(String.format(
            "Accès non autorisé: l'utilisateur %d n'est pas autorisé à %s la ressource %s",
            userId, action, resource
        ));
        this.userId = userId;
        this.resource = resource;
        this.action = action;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getAction() {
        return action;
    }
}
