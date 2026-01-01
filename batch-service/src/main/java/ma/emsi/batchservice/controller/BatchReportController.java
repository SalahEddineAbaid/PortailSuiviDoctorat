package ma.emsi.batchservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.emsi.batchservice.dto.ReportInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for batch report management.
 * Provides endpoints for listing and downloading generated monthly reports.
 * All endpoints require ROLE_ADMIN authorization.
 */
@RestController
@RequestMapping("/api/batch/reports")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Report Management", description = "APIs for listing and downloading generated monthly reports")
@SecurityRequirement(name = "Bearer Authentication")
public class BatchReportController {

    private static final Logger logger = LoggerFactory.getLogger(BatchReportController.class);

    @Value("${batch.reports.directory:./reports}")
    private String reportsDirectory;

    /**
     * List all generated monthly reports.
     *
     * @return List of reports with metadata and download links
     */
    @Operation(summary = "List all generated monthly reports", description = "Retrieves a list of all generated monthly reports with metadata including filename, generation date, file size, and download URL.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved report list", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReportInfoDTO.class), examples = @ExampleObject(value = """
                    [
                      {
                        "fileName": "rapport_2024_01.pdf",
                        "generatedDate": "2024-01-01T09:00:00",
                        "fileSize": 2458624,
                        "downloadUrl": "/api/batch/reports/rapport_2024_01.pdf/download"
                      }
                    ]
                    """))),
            @ApiResponse(responseCode = "403", description = "User does not have ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<ReportInfoDTO>> listReports() {
        logger.debug("Received request to list all reports");

        try {
            Path reportsPath = Paths.get(reportsDirectory);

            // Create directory if it doesn't exist
            if (!Files.exists(reportsPath)) {
                Files.createDirectories(reportsPath);
                return ResponseEntity.ok(new ArrayList<>());
            }

            // List all PDF files in the reports directory
            List<ReportInfoDTO> reports = Files.list(reportsPath)
                    .filter(path -> path.toString().endsWith(".pdf"))
                    .map(this::createReportInfo)
                    .filter(info -> info != null)
                    .sorted(Comparator.comparing(ReportInfoDTO::getGeneratedDate).reversed())
                    .collect(Collectors.toList());

            logger.info("Found {} reports", reports.size());
            return ResponseEntity.ok(reports);

        } catch (IOException e) {
            logger.error("Failed to list reports: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download a specific report PDF file.
     * Streams the file for efficient transfer of large files.
     *
     * @param fileName Name of the report file to download
     * @return Streaming response with PDF content
     */
    @Operation(summary = "Download a monthly report", description = """
            Downloads a specific monthly report PDF file. The file is streamed for efficient
            transfer of large files. Filename must be a valid PDF file without path traversal
            characters for security.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully streaming report file", content = @Content(mediaType = "application/pdf")),
            @ApiResponse(responseCode = "400", description = "Invalid filename (contains path traversal characters or not a PDF)", content = @Content),
            @ApiResponse(responseCode = "403", description = "User does not have ROLE_ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Report file not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during file streaming", content = @Content)
    })
    @GetMapping("/{fileName}/download")
    public ResponseEntity<StreamingResponseBody> downloadReport(
            @Parameter(description = "Name of the report file to download", required = true, example = "rapport_2024_01.pdf") @PathVariable String fileName) {
        logger.info("Received request to download report: {}", fileName);

        // Validate filename to prevent directory traversal attacks
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            logger.warn("Invalid filename detected: {}", fileName);
            return ResponseEntity.badRequest().build();
        }

        // Ensure filename ends with .pdf
        if (!fileName.endsWith(".pdf")) {
            logger.warn("Non-PDF file requested: {}", fileName);
            return ResponseEntity.badRequest().build();
        }

        Path filePath = Paths.get(reportsDirectory, fileName);
        File file = filePath.toFile();

        if (!file.exists() || !file.isFile()) {
            logger.warn("Report file not found: {}", fileName);
            return ResponseEntity.notFound().build();
        }

        try {
            // Create streaming response body
            StreamingResponseBody stream = outputStream -> {
                try (InputStream inputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    logger.error("Error streaming file {}: {}", fileName, e.getMessage(), e);
                    throw new RuntimeException("Error streaming file", e);
                }
            };

            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentLength(file.length());
            headers.setContentDispositionFormData("attachment", fileName);

            logger.info("Streaming report file: {} ({} bytes)", fileName, file.length());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(stream);

        } catch (Exception e) {
            logger.error("Failed to download report {}: {}", fileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create ReportInfoDTO from a file path.
     */
    private ReportInfoDTO createReportInfo(Path path) {
        try {
            File file = path.toFile();
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

            LocalDateTime generatedDate = LocalDateTime.ofInstant(
                    attrs.creationTime().toInstant(),
                    ZoneId.systemDefault());

            String downloadUrl = "/api/batch/reports/" + file.getName() + "/download";

            return new ReportInfoDTO(
                    file.getName(),
                    generatedDate,
                    file.length(),
                    downloadUrl);
        } catch (IOException e) {
            logger.error("Failed to read file attributes for {}: {}", path, e.getMessage());
            return null;
        }
    }
}
