package ma.emsi.inscriptionservice.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when document validation fails.
 * Includes specific error codes for different validation failures.
 */
public class InvalidDocumentException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    
    public InvalidDocumentException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
    
    public InvalidDocumentException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    // Predefined error codes
    public static final String INVALID_MIME_TYPE = "INVALID_DOCUMENT_TYPE";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String VIRUS_DETECTED = "VIRUS_DETECTED";
    public static final String INVALID_FILE_NAME = "INVALID_FILE_NAME";
    public static final String FILE_EMPTY = "FILE_EMPTY";
}
