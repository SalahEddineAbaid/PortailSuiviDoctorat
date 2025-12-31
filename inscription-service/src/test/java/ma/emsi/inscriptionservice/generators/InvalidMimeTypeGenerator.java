package ma.emsi.inscriptionservice.generators;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Custom generator for invalid MIME types for property-based testing.
 * Generates only MIME types that should be rejected by document validation.
 */
public class InvalidMimeTypeGenerator extends Generator<String> {

    private static final String[] INVALID_MIME_TYPES = {
        // Office documents
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        
        // Text formats
        "text/plain",
        "text/html",
        "text/css",
        "text/javascript",
        "application/javascript",
        "application/json",
        "application/xml",
        "text/xml",
        
        // Archives
        "application/zip",
        "application/x-rar-compressed",
        "application/x-7z-compressed",
        "application/x-tar",
        "application/gzip",
        
        // Executables
        "application/x-executable",
        "application/x-msdownload",
        "application/x-sh",
        "application/x-bat",
        
        // Media (video/audio)
        "video/mp4",
        "video/mpeg",
        "video/quicktime",
        "audio/mpeg",
        "audio/wav",
        "audio/ogg",
        
        // Other images (not allowed)
        "image/gif",
        "image/bmp",
        "image/svg+xml",
        "image/webp",
        "image/tiff",
        
        // Other
        "application/octet-stream",
        "application/x-binary",
        "application/unknown",
        
        // Malformed MIME types
        "invalid/type",
        "application/",
        "/pdf",
        "pdf",
        "",
        "text",
        "application",
        "image/jpg", // Note: correct is image/jpeg
        "application/pdf ", // with trailing space
        " application/pdf", // with leading space
        "APPLICATION/PDF", // uppercase
        "application/PDF",
        "Application/Pdf"
    };

    public InvalidMimeTypeGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        return random.choose(INVALID_MIME_TYPES);
    }
}
