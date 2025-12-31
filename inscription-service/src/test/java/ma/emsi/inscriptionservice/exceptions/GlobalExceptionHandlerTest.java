package ma.emsi.inscriptionservice.exceptions;

import ma.emsi.inscriptionservice.DTOs.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests error response format, HTTP status codes, and error messages.
 */
class GlobalExceptionHandlerTest {
    
    private GlobalExceptionHandler exceptionHandler;
    private WebRequest webRequest;
    
    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        webRequest = new ServletWebRequest(request);
    }
    
    @Test
    void handleInvalidDocument_ShouldReturnBadRequest() {
        // Arrange
        InvalidDocumentException exception = new InvalidDocumentException(
            "Le fichier doit être au format PDF ou image (JPEG/PNG)",
            InvalidDocumentException.INVALID_MIME_TYPE
        );
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidDocument(exception, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals(InvalidDocumentException.INVALID_MIME_TYPE, errorResponse.getErrorCode());
        assertEquals("Le fichier doit être au format PDF ou image (JPEG/PNG)", errorResponse.getMessage());
        assertTrue(errorResponse.getPath().contains("/api/test"));
        assertNotNull(errorResponse.getTimestamp());
    }
    
    @Test
    void handleInvalidDocument_FileTooLarge_ShouldReturnCorrectErrorCode() {
        // Arrange
        InvalidDocumentException exception = new InvalidDocumentException(
            "La taille du fichier ne doit pas dépasser 10 MB",
            InvalidDocumentException.FILE_TOO_LARGE
        );
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidDocument(exception, webRequest);
        
        // Assert
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(InvalidDocumentException.FILE_TOO_LARGE, errorResponse.getErrorCode());
        assertEquals("La taille du fichier ne doit pas dépasser 10 MB", errorResponse.getMessage());
    }
    
    @Test
    void handleDerogationRequired_ShouldReturnForbidden() {
        // Arrange
        DerogationRequiredException exception = new DerogationRequiredException(1L, 100L, 3.5);
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDerogationRequired(exception, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("Forbidden", errorResponse.getError());
        assertEquals("DEROGATION_REQUIRED", errorResponse.getErrorCode());
        assertTrue(errorResponse.getMessage().contains("dérogation"));
        assertTrue(errorResponse.getMessage().contains("3.5 ans"));
        assertNotNull(errorResponse.getTimestamp());
    }
    
    @Test
    void handleDurationLimit_ShouldReturnForbidden() {
        // Arrange
        DurationLimitExceededException exception = new DurationLimitExceededException(1L, 100L, 6.2);
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDurationLimit(exception, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("Forbidden", errorResponse.getError());
        assertEquals("DURATION_LIMIT_EXCEEDED", errorResponse.getErrorCode());
        assertTrue(errorResponse.getMessage().contains("6 ans"));
        assertTrue(errorResponse.getMessage().contains("6.2 ans"));
        assertNotNull(errorResponse.getTimestamp());
    }
    
    @Test
    void handleUnauthorizedAccess_ShouldReturnForbidden() {
        // Arrange
        UnauthorizedAccessException exception = new UnauthorizedAccessException(
            100L, "inscription", "modifier"
        );
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorizedAccess(exception, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("Forbidden", errorResponse.getError());
        assertEquals("UNAUTHORIZED_ACCESS", errorResponse.getErrorCode());
        assertTrue(errorResponse.getMessage().contains("Accès non autorisé"));
        assertNotNull(errorResponse.getTimestamp());
    }
    
    @Test
    void handleCampagneClosed_ShouldReturnBadRequest() {
        // Arrange
        CampagneClosedException exception = new CampagneClosedException(
            1L, "Campagne 2023-2024", LocalDate.of(2024, 6, 30)
        );
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleCampagneClosed(exception, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("CAMPAGNE_CLOSED", errorResponse.getErrorCode());
        assertTrue(errorResponse.getMessage().contains("fermée"));
        assertNotNull(errorResponse.getTimestamp());
    }
    
    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error");
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception, webRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(500, errorResponse.getStatus());
        assertEquals("Internal Server Error", errorResponse.getError());
        assertEquals("INTERNAL_ERROR", errorResponse.getErrorCode());
        assertEquals("Une erreur interne s'est produite. Veuillez réessayer plus tard.", errorResponse.getMessage());
        assertNotNull(errorResponse.getTimestamp());
    }
    
    @Test
    void errorResponse_ShouldFollowStandardFormat() {
        // Arrange
        InvalidDocumentException exception = new InvalidDocumentException(
            "Test message",
            "TEST_CODE"
        );
        
        // Act
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidDocument(exception, webRequest);
        
        // Assert - Verify all required fields are present
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.getTimestamp(), "timestamp should not be null");
        assertTrue(errorResponse.getStatus() > 0, "status should be positive");
        assertNotNull(errorResponse.getError(), "error should not be null");
        assertNotNull(errorResponse.getErrorCode(), "errorCode should not be null");
        assertNotNull(errorResponse.getMessage(), "message should not be null");
        assertNotNull(errorResponse.getPath(), "path should not be null");
    }
}
