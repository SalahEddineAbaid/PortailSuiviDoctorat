package ma.emsi.defenseservice.service;

import ma.emsi.defenseservice.dto.request.FinalizationDTO;
import ma.emsi.defenseservice.entity.Defense;
import ma.emsi.defenseservice.entity.JuryMember;
import ma.emsi.defenseservice.enums.DefenseStatus;
import ma.emsi.defenseservice.exception.ResourceNotFoundException;
import ma.emsi.defenseservice.exception.UnauthorizedAccessException;
import ma.emsi.defenseservice.repository.DefenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing defense operations including finalization and
 * procès-verbal generation
 * Implements Requirements 3.1, 3.2, 3.5, 3.7, 3.8
 */
@Service
public class DefenseService {

    private static final Logger logger = LoggerFactory.getLogger(DefenseService.class);
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Autowired
    private DefenseRepository defenseRepository;

    @Autowired
    private ProcesVerbalPdfGenerator procesVerbalPdfGenerator;

    @Autowired
    private UserServiceFacade userServiceFacade;

    @Autowired
    private DefenseEventPublisher defenseEventPublisher;

    @Value("${defense.pv.storage.path:./storage/proces-verbaux}")
    private String pvStoragePath;

    @Value("${defense.pv.base-url:http://localhost:8083/api/defense-service/defenses}")
    private String pvBaseUrl;

    public Defense schedule(Defense d) {
        return defenseRepository.save(d);
    }

    public Defense getByDefenseRequest(Long requestId) {
        return defenseRepository.findByDefenseRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Defense not found"));
    }

    public Defense updateStatus(Long id, DefenseStatus status) {
        Defense defense = defenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Defense not found"));

        defense.setStatus(status);
        return defenseRepository.save(defense);
    }

    /**
     * Finalize a defense with outcome data and generate procès-verbal PDF
     * Implements Requirements 3.1, 3.2, 3.5
     * 
     * @param id              Defense ID
     * @param finalizationDTO Finalization data (mention, publication
     *                        recommendation, comments, deliberation date)
     * @return Updated defense entity
     */
    @Transactional
    public Defense finalizeDefense(Long id, FinalizationDTO finalizationDTO) {
        logger.info("🎓 Finalizing defense ID: {}", id);

        // Retrieve defense
        Defense defense = defenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Defense not found with ID: " + id));

        // Validate defense can be finalized
        if (defense.getStatus() == DefenseStatus.COMPLETED) {
            throw new IllegalStateException("Defense is already finalized");
        }

        if (defense.getStatus() != DefenseStatus.SCHEDULED && defense.getStatus() != DefenseStatus.IN_PROGRESS) {
            throw new IllegalStateException("Defense must be scheduled or in progress to be finalized");
        }

        // Update defense with finalization data (Requirement 3.1)
        defense.setMention(finalizationDTO.getMention());
        defense.setPublicationRecommended(finalizationDTO.getPublicationRecommended());
        defense.setJuryComments(finalizationDTO.getJuryComments());
        defense.setDeliberationDate(finalizationDTO.getDeliberationDate());

        // Change status to COMPLETED (Requirement 3.5)
        defense.setStatus(DefenseStatus.COMPLETED);

        // Generate procès-verbal PDF (Requirement 3.2)
        try {
            byte[] pdfBytes = procesVerbalPdfGenerator.generateProcesVerbal(defense);

            // Store PDF and get URL
            String pdfUrl = storeProcesVerbalPdf(defense.getId(), pdfBytes);
            defense.setProcesVerbalUrl(pdfUrl);

            logger.info("✅ Procès-verbal stored at: {}", pdfUrl);
        } catch (Exception e) {
            logger.error("❌ Failed to generate or store procès-verbal for defense ID: {}", id, e);
            throw new RuntimeException("Failed to generate procès-verbal: " + e.getMessage(), e);
        }

        // Save updated defense
        Defense finalizedDefense = defenseRepository.save(defense);
        logger.info("✅ Defense finalized successfully with mention: {}", finalizationDTO.getMention());

        // Publish SOUTENANCE_FINALISEE event (Requirement 3.6)
        defenseEventPublisher.publishDefenseFinalized(finalizedDefense);

        return finalizedDefense;
    }

    /**
     * Get procès-verbal PDF with access control
     * Implements Requirements 3.7, 3.8
     * 
     * @param defenseId Defense ID
     * @param userId    User requesting the PV
     * @return PDF bytes
     */
    public byte[] getProcesVerbal(Long defenseId, Long userId) {
        logger.info("📄 User {} requesting procès-verbal for defense ID: {}", userId, defenseId);

        // Retrieve defense
        Defense defense = defenseRepository.findById(defenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Defense not found with ID: " + defenseId));

        // Check if defense is finalized (Requirement 3.8)
        if (defense.getStatus() != DefenseStatus.COMPLETED) {
            throw new IllegalStateException("Procès-verbal is not available. Defense is not yet completed.");
        }

        if (defense.getProcesVerbalUrl() == null || defense.getProcesVerbalUrl().isEmpty()) {
            throw new IllegalStateException("Procès-verbal has not been generated yet");
        }

        // Verify access control (Requirement 3.7)
        if (!hasAccessToProcesVerbal(defense, userId)) {
            logger.warn("⚠️ Unauthorized access attempt by user {} for defense ID: {}", userId, defenseId);
            throw new UnauthorizedAccessException(
                    "You do not have permission to access this procès-verbal. " +
                            "Access is restricted to jury members, the doctorant, the directeur, and administrators.");
        }

        // Read and return PDF
        try {
            String filename = extractFilenameFromUrl(defense.getProcesVerbalUrl());
            Path pdfPath = Paths.get(pvStoragePath, filename);

            if (!Files.exists(pdfPath)) {
                throw new ResourceNotFoundException("Procès-verbal file not found on disk");
            }

            logger.info("✅ Procès-verbal access granted to user {} for defense ID: {}", userId, defenseId);
            return Files.readAllBytes(pdfPath);
        } catch (IOException e) {
            logger.error("❌ Failed to read procès-verbal file for defense ID: {}", defenseId, e);
            throw new RuntimeException("Failed to read procès-verbal file: " + e.getMessage(), e);
        }
    }

    /**
     * Check if user has access to procès-verbal
     * Access granted to: jury members, doctorant, directeur, admin
     * Implements Requirement 3.7
     */
    private boolean hasAccessToProcesVerbal(Defense defense, Long userId) {
        // Check if user is the doctorant
        Long doctorantId = defense.getDefenseRequest().getDoctorantId();
        if (userId.equals(doctorantId)) {
            logger.debug("✓ Access granted: User is the doctorant");
            return true;
        }

        // Check if user is the directeur (from jury)
        if (defense.getDefenseRequest().getJury() != null) {
            Long directeurId = defense.getDefenseRequest().getJury().getDirectorId();
            if (userId.equals(directeurId)) {
                logger.debug("✓ Access granted: User is the directeur");
                return true;
            }

            // Check if user is a jury member
            List<Long> juryMemberIds = defense.getDefenseRequest().getJury().getMembers()
                    .stream()
                    .map(JuryMember::getProfessorId)
                    .collect(Collectors.toList());

            if (juryMemberIds.contains(userId)) {
                logger.debug("✓ Access granted: User is a jury member");
                return true;
            }
        }

        // Check if user is an admin
        try {
            boolean isAdmin = userServiceFacade.validateUserRole(userId, "ADMIN");
            if (isAdmin) {
                logger.debug("✓ Access granted: User is an admin");
                return true;
            }
        } catch (Exception e) {
            logger.warn("⚠️ Failed to validate admin role for user {}: {}", userId, e.getMessage());
            // Continue to deny access if role validation fails
        }

        logger.debug("✗ Access denied: User does not have required permissions");
        return false;
    }

    /**
     * Store procès-verbal PDF to filesystem and return URL
     */
    private String storeProcesVerbalPdf(Long defenseId, byte[] pdfBytes) throws IOException {
        // Create storage directory if it doesn't exist
        Path storagePath = Paths.get(pvStoragePath);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
            logger.info("📁 Created storage directory: {}", pvStoragePath);
        }

        // Generate unique filename
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
        String filename = String.format("pv_defense_%d_%s.pdf", defenseId, timestamp);
        Path filePath = storagePath.resolve(filename);

        // Write PDF to file
        Files.write(filePath, pdfBytes);
        logger.info("💾 Procès-verbal saved to: {}", filePath);

        // Return URL
        return String.format("%s/%d/proces-verbal", pvBaseUrl, defenseId);
    }

    /**
     * Extract filename from URL for file retrieval
     */
    private String extractFilenameFromUrl(String url) {
        // URL format:
        // http://localhost:8083/api/defense-service/defenses/{id}/proces-verbal
        // We need to find the actual file on disk
        // For now, we'll search for files matching the defense ID pattern
        try {
            Path storagePath = Paths.get(pvStoragePath);
            if (!Files.exists(storagePath)) {
                throw new ResourceNotFoundException("Storage directory not found");
            }

            // Extract defense ID from URL
            String[] parts = url.split("/");
            String defenseIdStr = parts[parts.length - 2];
            Long defenseId = Long.parseLong(defenseIdStr);

            // Find file matching pattern
            String pattern = String.format("pv_defense_%d_", defenseId);
            return Files.list(storagePath)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith(pattern))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Procès-verbal file not found"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to locate procès-verbal file", e);
        }
    }
}
