package ma.emsi.inscriptionservice.services;

import ma.emsi.inscriptionservice.enums.TypeDocument;
import ma.emsi.inscriptionservice.exceptions.InvalidDocumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DocumentValidationService.
 */
class DocumentValidationServiceTest {
    
    private DocumentValidationService documentValidationService;
    
    @BeforeEach
    void setUp() {
        documentValidationService = new DocumentValidationService();
        
        // Set configuration values using reflection
        ReflectionTestUtils.setField(documentValidationService, "allowedTypesConfig", 
                                     "application/pdf,image/jpeg,image/png");
        ReflectionTestUtils.setField(documentValidationService, "maxFileSize", 10485760L); // 10 MB
        ReflectionTestUtils.setField(documentValidationService, "virusScanEnabled", false);
    }
    
    @Test
    void testIsMimeTypeAllowed_ValidPdf() {
        assertTrue(documentValidationService.isMimeTypeAllowed("application/pdf"));
    }
    
    @Test
    void testIsMimeTypeAllowed_ValidJpeg() {
        assertTrue(documentValidationService.isMimeTypeAllowed("image/jpeg"));
    }
    
    @Test
    void testIsMimeTypeAllowed_ValidPng() {
        assertTrue(documentValidationService.isMimeTypeAllowed("image/png"));
    }
    
    @Test
    void testIsMimeTypeAllowed_InvalidType() {
        assertFalse(documentValidationService.isMimeTypeAllowed("application/msword"));
        assertFalse(documentValidationService.isMimeTypeAllowed("text/plain"));
        assertFalse(documentValidationService.isMimeTypeAllowed("application/zip"));
    }
    
    @Test
    void testIsMimeTypeAllowed_NullType() {
        assertFalse(documentValidationService.isMimeTypeAllowed(null));
    }
    
    @Test
    void testIsMimeTypeAllowed_EmptyType() {
        assertFalse(documentValidationService.isMimeTypeAllowed(""));
        assertFalse(documentValidationService.isMimeTypeAllowed("   "));
    }
    
    @Test
    void testIsFileSizeValid_ValidSize() {
        assertTrue(documentValidationService.isFileSizeValid(1024)); // 1 KB
        assertTrue(documentValidationService.isFileSizeValid(5242880)); // 5 MB
        assertTrue(documentValidationService.isFileSizeValid(10485760)); // Exactly 10 MB
    }
    
    @Test
    void testIsFileSizeValid_TooLarge() {
        assertFalse(documentValidationService.isFileSizeValid(10485761)); // 10 MB + 1 byte
        assertFalse(documentValidationService.isFileSizeValid(20971520)); // 20 MB
    }
    
    @Test
    void testIsFileSizeValid_ZeroOrNegative() {
        assertFalse(documentValidationService.isFileSizeValid(0));
        assertFalse(documentValidationService.isFileSizeValid(-1));
    }
    
    @Test
    void testGenerateSecureFileName_WithoutExtension() {
        String fileName = documentValidationService.generateSecureFileName(TypeDocument.CV, 12345L);
        
        assertNotNull(fileName);
        assertTrue(fileName.startsWith("cv_"));
        assertTrue(fileName.contains("_12345_"));
        
        // Verify pattern: {type}_{timestamp}_{userId}_{random}
        String[] parts = fileName.split("_");
        assertEquals(4, parts.length);
        assertEquals("cv", parts[0]);
        assertEquals("12345", parts[2]);
    }
    
    @Test
    void testGenerateSecureFileName_WithExtension() {
        String fileName = documentValidationService.generateSecureFileName(
            TypeDocument.DIPLOME_MASTER, 67890L, "document.pdf"
        );
        
        assertNotNull(fileName);
        assertTrue(fileName.startsWith("diplome_master_"));
        assertTrue(fileName.contains("_67890_"));
        assertTrue(fileName.endsWith(".pdf"));
    }
    
    @Test
    void testValidateDocument_ValidPdf() {
        byte[] content = new byte[1024]; // 1 KB
        MultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", content
        );
        
        assertDoesNotThrow(() -> 
            documentValidationService.validateDocument(file, TypeDocument.CV)
        );
    }
    
    @Test
    void testValidateDocument_InvalidMimeType() {
        byte[] content = new byte[1024];
        MultipartFile file = new MockMultipartFile(
            "file", "test.doc", "application/msword", content
        );
        
        InvalidDocumentException exception = assertThrows(
            InvalidDocumentException.class,
            () -> documentValidationService.validateDocument(file, TypeDocument.CV)
        );
        
        assertEquals("Le fichier doit être au format PDF ou image (JPEG/PNG)", 
                    exception.getMessage());
        assertEquals(InvalidDocumentException.INVALID_MIME_TYPE, exception.getErrorCode());
    }
    
    @Test
    void testValidateDocument_FileTooLarge() {
        byte[] content = new byte[10485761]; // 10 MB + 1 byte
        MultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", content
        );
        
        InvalidDocumentException exception = assertThrows(
            InvalidDocumentException.class,
            () -> documentValidationService.validateDocument(file, TypeDocument.CV)
        );
        
        assertEquals("La taille du fichier ne doit pas dépasser 10 MB", 
                    exception.getMessage());
        assertEquals(InvalidDocumentException.FILE_TOO_LARGE, exception.getErrorCode());
    }
    
    @Test
    void testValidateDocument_EmptyFile() {
        MultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", new byte[0]
        );
        
        InvalidDocumentException exception = assertThrows(
            InvalidDocumentException.class,
            () -> documentValidationService.validateDocument(file, TypeDocument.CV)
        );
        
        assertEquals("Le fichier est vide", exception.getMessage());
        assertEquals(InvalidDocumentException.FILE_EMPTY, exception.getErrorCode());
    }
    
    @Test
    void testValidateDocument_ExactlyMaxSize() {
        byte[] content = new byte[10485760]; // Exactly 10 MB
        MultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", content
        );
        
        assertDoesNotThrow(() -> 
            documentValidationService.validateDocument(file, TypeDocument.CV)
        );
    }
}
