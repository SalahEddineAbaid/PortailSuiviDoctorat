package ma.emsi.inscriptionservice.services;

import ma.emsi.inscriptionservice.enums.TypeDocument;
import ma.emsi.inscriptionservice.exceptions.InvalidDocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for validating uploaded documents.
 * Validates MIME type, file size, and optionally scans for viruses.
 */
@Service
public class DocumentValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DocumentValidationService.class);
    
    @Value("${upload.allowed-types}")
    private String allowedTypesConfig;
    
    @Value("${upload.max-size}")
    private long maxFileSize;
    
    @Value("${upload.virus-scan.enabled:false}")
    private boolean virusScanEnabled;
    
    @Value("${upload.virus-scan.clamav.host:localhost}")
    private String clamavHost;
    
    @Value("${upload.virus-scan.clamav.port:3310}")
    private int clamavPort;
    
    /**
     * Validates a document for MIME type, size, and optionally virus scanning.
     * 
     * @param file The uploaded file to validate
     * @param type The type of document being uploaded
     * @throws InvalidDocumentException if validation fails
     */
    public void validateDocument(MultipartFile file, TypeDocument type) {
        logger.debug("Validating document of type {} with MIME type {} and size {}", 
                     type, file.getContentType(), file.getSize());
        
        // Check if file is empty
        if (file.isEmpty()) {
            throw new InvalidDocumentException(
                "Le fichier est vide",
                InvalidDocumentException.FILE_EMPTY
            );
        }
        
        // Validate MIME type
        if (!isMimeTypeAllowed(file.getContentType())) {
            throw new InvalidDocumentException(
                "Le fichier doit être au format PDF ou image (JPEG/PNG)",
                InvalidDocumentException.INVALID_MIME_TYPE
            );
        }
        
        // Validate file size
        if (!isFileSizeValid(file.getSize())) {
            throw new InvalidDocumentException(
                "La taille du fichier ne doit pas dépasser 10 MB",
                InvalidDocumentException.FILE_TOO_LARGE
            );
        }
        
        // Optional virus scanning
        if (virusScanEnabled) {
            scanForVirus(file);
        }
        
        logger.debug("Document validation successful for type {}", type);
    }
    
    /**
     * Checks if the MIME type is in the allowed list.
     * 
     * @param mimeType The MIME type to check
     * @return true if allowed, false otherwise
     */
    public boolean isMimeTypeAllowed(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return false;
        }
        
        List<String> allowedTypes = Arrays.asList(allowedTypesConfig.split(","));
        return allowedTypes.stream()
                .map(String::trim)
                .anyMatch(allowed -> allowed.equalsIgnoreCase(mimeType.trim()));
    }
    
    /**
     * Checks if the file size is within the allowed limit.
     * 
     * @param size The file size in bytes
     * @return true if valid, false otherwise
     */
    public boolean isFileSizeValid(long size) {
        return size > 0 && size <= maxFileSize;
    }
    
    /**
     * Generates a secure file name using a pattern to prevent conflicts and security issues.
     * Pattern: {type}_{timestamp}_{userId}_{random}.{extension}
     * 
     * @param type The document type
     * @param userId The user ID
     * @return A secure file name
     */
    public String generateSecureFileName(TypeDocument type, Long userId) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String random = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("%s_%s_%s_%s", 
                           type.name().toLowerCase(),
                           timestamp,
                           userId,
                           random);
    }
    
    /**
     * Generates a secure file name with extension.
     * Pattern: {type}_{timestamp}_{userId}_{random}.{extension}
     * 
     * @param type The document type
     * @param userId The user ID
     * @param originalFilename The original filename to extract extension
     * @return A secure file name with extension
     */
    public String generateSecureFileName(TypeDocument type, Long userId, String originalFilename) {
        String baseName = generateSecureFileName(type, userId);
        String extension = getFileExtension(originalFilename);
        
        if (extension != null && !extension.isEmpty()) {
            return baseName + "." + extension;
        }
        
        return baseName;
    }
    
    /**
     * Extracts the file extension from a filename.
     * 
     * @param filename The filename
     * @return The extension without the dot, or empty string if no extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return "";
    }
    
    /**
     * Scans a file for viruses using ClamAV.
     * This is an optional feature that can be enabled via configuration.
     * 
     * @param file The file to scan
     * @throws InvalidDocumentException if a virus is detected or scanning fails
     */
    public void scanForVirus(MultipartFile file) {
        if (!virusScanEnabled) {
            logger.debug("Virus scanning is disabled");
            return;
        }
        
        logger.debug("Scanning file for viruses using ClamAV at {}:{}", clamavHost, clamavPort);
        
        try (Socket socket = new Socket(clamavHost, clamavPort);
             InputStream inputStream = file.getInputStream()) {
            
            // Send INSTREAM command to ClamAV
            socket.getOutputStream().write("zINSTREAM\0".getBytes());
            
            // Send file data in chunks
            byte[] buffer = new byte[2048];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // Send chunk size (4 bytes, network byte order)
                byte[] chunkSize = ByteBuffer.allocate(4).putInt(bytesRead).array();
                socket.getOutputStream().write(chunkSize);
                
                // Send chunk data
                socket.getOutputStream().write(buffer, 0, bytesRead);
            }
            
            // Send zero-length chunk to indicate end of stream
            socket.getOutputStream().write(new byte[]{0, 0, 0, 0});
            socket.getOutputStream().flush();
            
            // Read response from ClamAV
            byte[] response = new byte[1024];
            int responseLength = socket.getInputStream().read(response);
            String result = new String(response, 0, responseLength).trim();
            
            logger.debug("ClamAV scan result: {}", result);
            
            // Check if virus was found
            if (!result.contains("OK")) {
                logger.warn("Virus detected in uploaded file: {}", result);
                throw new InvalidDocumentException(
                    "Le fichier contient un virus ou un contenu malveillant",
                    InvalidDocumentException.VIRUS_DETECTED
                );
            }
            
            logger.debug("Virus scan completed successfully - file is clean");
            
        } catch (IOException e) {
            logger.error("Error during virus scanning: {}", e.getMessage(), e);
            // Don't block upload if virus scanning fails, just log the error
            logger.warn("Virus scanning failed, allowing upload to proceed");
        }
    }
}
