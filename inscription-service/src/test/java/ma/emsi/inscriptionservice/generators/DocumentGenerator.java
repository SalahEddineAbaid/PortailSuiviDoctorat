package ma.emsi.inscriptionservice.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Custom generator for MultipartFile (document uploads) for property-based testing.
 * Generates random documents with various MIME types and sizes, including edge cases.
 */
public class DocumentGenerator extends Generator<MultipartFile> {

    private static final String[] VALID_MIME_TYPES = {
        "application/pdf",
        "image/jpeg",
        "image/png"
    };
    
    private static final String[] INVALID_MIME_TYPES = {
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "application/zip",
        "application/x-executable",
        "text/html",
        "application/javascript"
    };
    
    private static final String[] ALL_MIME_TYPES;
    
    static {
        ALL_MIME_TYPES = new String[VALID_MIME_TYPES.length + INVALID_MIME_TYPES.length];
        System.arraycopy(VALID_MIME_TYPES, 0, ALL_MIME_TYPES, 0, VALID_MIME_TYPES.length);
        System.arraycopy(INVALID_MIME_TYPES, 0, ALL_MIME_TYPES, VALID_MIME_TYPES.length, INVALID_MIME_TYPES.length);
    }

    public DocumentGenerator() {
        super(MultipartFile.class);
    }

    @Override
    public MultipartFile generate(SourceOfRandomness random, GenerationStatus status) {
        // Choose a random MIME type (mix of valid and invalid)
        String mimeType = random.choose(ALL_MIME_TYPES);
        
        // Generate file size with various edge cases
        long fileSize = generateFileSize(random);
        
        // Generate file content
        byte[] content = new byte[(int) fileSize];
        random.nextBytes(content);
        
        // Generate filename with appropriate extension
        String extension = getExtensionForMimeType(mimeType);
        String filename = "document_" + random.nextInt(1, 10000) + extension;
        
        return new MockMultipartFile(
            "file",
            filename,
            mimeType,
            content
        );
    }
    
    /**
     * Generates file sizes including edge cases:
     * - Very small files (< 1KB)
     * - Normal files (1KB - 5MB)
     * - Large files (5MB - 9.9MB)
     * - Boundary cases (exactly 10MB, 10MB + 1 byte)
     * - Oversized files (> 10MB)
     */
    private long generateFileSize(SourceOfRandomness random) {
        int sizeCategory = random.nextInt(0, 100);
        
        if (sizeCategory < 10) {
            // Very small files (10% chance)
            return random.nextLong(1, 1024); // < 1KB
        } else if (sizeCategory < 50) {
            // Normal files (40% chance)
            return random.nextLong(1024, 5 * 1024 * 1024); // 1KB - 5MB
        } else if (sizeCategory < 75) {
            // Large files (25% chance)
            return random.nextLong(5 * 1024 * 1024, 10 * 1024 * 1024 - 1024); // 5MB - 9.9MB
        } else if (sizeCategory < 80) {
            // Exactly 10MB (5% chance) - boundary case
            return 10 * 1024 * 1024;
        } else if (sizeCategory < 85) {
            // 10MB + 1 byte (5% chance) - boundary case
            return 10 * 1024 * 1024 + 1;
        } else {
            // Oversized files (15% chance)
            return random.nextLong(10 * 1024 * 1024 + 2, 20 * 1024 * 1024); // 10MB - 20MB
        }
    }
    
    /**
     * Returns appropriate file extension for the given MIME type.
     */
    private String getExtensionForMimeType(String mimeType) {
        switch (mimeType) {
            case "application/pdf":
                return ".pdf";
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "application/msword":
                return ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                return ".docx";
            case "text/plain":
                return ".txt";
            case "application/zip":
                return ".zip";
            case "application/x-executable":
                return ".exe";
            case "text/html":
                return ".html";
            case "application/javascript":
                return ".js";
            default:
                return ".bin";
        }
    }
}
