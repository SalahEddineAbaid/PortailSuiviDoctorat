package ma.emsi.inscriptionservice.exceptions;

import ma.emsi.inscriptionservice.DTOs.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

/**
 * Global exception handler for the inscription-service.
 * Handles all exceptions and returns standardized error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles InvalidDocumentException thrown during document validation.
     * Returns BAD_REQUEST (400) with specific error code and message.
     */
    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDocument(
            InvalidDocumentException ex, 
            WebRequest request) {
        
        logger.warn("Invalid document: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getHttpStatus().value())
                .error(ex.getHttpStatus().getReasonPhrase())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        
        return new ResponseEntity<>(errorResponse, ex.getHttpStatus());
    }
    
    /**
     * Handles DerogationRequiredException thrown when re-registration requires derogation.
     * Returns FORBIDDEN (403) with details about the derogation requirement.
     */
    @ExceptionHandler(DerogationRequiredException.class)
    public ResponseEntity<ErrorResponse> handleDerogationRequired(
            DerogationRequiredException ex, 
            WebRequest request) {
        
        logger.warn("Derogation required for doctorant {} - Duration: {} years", 
                ex.getDoctorantId(), ex.getDureeAnnees());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .errorCode("DEROGATION_REQUIRED")
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handles DurationLimitExceededException thrown when 6-year limit is exceeded.
     * Returns FORBIDDEN (403) indicating re-registration is blocked.
     */
    @ExceptionHandler(DurationLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDurationLimit(
            DurationLimitExceededException ex, 
            WebRequest request) {
        
        logger.error("Duration limit exceeded for doctorant {} - Duration: {} years", 
                ex.getDoctorantId(), ex.getDureeAnnees());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .errorCode("DURATION_LIMIT_EXCEEDED")
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handles UnauthorizedAccessException thrown when user lacks authorization.
     * Returns FORBIDDEN (403) with authorization failure details.
     */
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
            UnauthorizedAccessException ex, 
            WebRequest request) {
        
        logger.warn("Unauthorized access attempt: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .errorCode("UNAUTHORIZED_ACCESS")
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handles CampagneClosedException thrown when attempting to register in closed campaign.
     * Returns BAD_REQUEST (400) with campaign closure details.
     */
    @ExceptionHandler(CampagneClosedException.class)
    public ResponseEntity<ErrorResponse> handleCampagneClosed(
            CampagneClosedException ex, 
            WebRequest request) {
        
        logger.warn("Attempt to register in closed campaign: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .errorCode("CAMPAGNE_CLOSED")
                .message(ex.getMessage())
                .path(extractPath(request))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handles all other exceptions not specifically handled above.
     * Returns INTERNAL_SERVER_ERROR (500) with generic error message.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, 
            WebRequest request) {
        
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .errorCode("INTERNAL_ERROR")
                .message("Une erreur interne s'est produite. Veuillez réessayer plus tard.")
                .path(extractPath(request))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Extracts the request path from WebRequest.
     */
    private String extractPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
