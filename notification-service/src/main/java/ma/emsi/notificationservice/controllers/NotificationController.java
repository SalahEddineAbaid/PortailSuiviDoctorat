package ma.emsi.notificationservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.notificationservice.dtos.NotificationStatsDTO;
import ma.emsi.notificationservice.entities.Notification;
import ma.emsi.notificationservice.entities.NotificationDLQ;
import ma.emsi.notificationservice.enums.StatutNotification;
import ma.emsi.notificationservice.enums.TypeNotification;
import ma.emsi.notificationservice.repositories.NotificationDLQRepository;
import ma.emsi.notificationservice.repositories.NotificationRepository;
import ma.emsi.notificationservice.services.NotificationHistoryService;
import ma.emsi.notificationservice.services.NotificationProcessingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for notification management.
 * Provides endpoints for querying notification history, statistics, and retry
 * operations.
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 11.4
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "APIs for managing email notifications, querying history, and monitoring delivery status")
@SecurityRequirement(name = "Bearer Authentication")
public class NotificationController {

    private final NotificationHistoryService notificationHistoryService;
    private final NotificationProcessingService notificationProcessingService;
    private final NotificationRepository notificationRepository;
    private final NotificationDLQRepository notificationDLQRepository;

    /**
     * Get all notifications with pagination.
     * Requirement 6.1: WHEN an admin requests GET /api/notifications THEN return a
     * paginated list of all notifications
     */
    @Operation(summary = "Get all notifications", description = "Retrieve a paginated list of all notifications. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved notifications", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Notification>> getAllNotifications(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Field to sort by", example = "dateCreation") @RequestParam(defaultValue = "dateCreation") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)", example = "DESC") @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("GET /api/notifications - page: {}, size: {}, sortBy: {}, sortDir: {}",
                page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Notification> notifications = notificationRepository.findAll(pageable);

        log.info("Retrieved {} notifications (page {} of {})",
                notifications.getNumberOfElements(),
                notifications.getNumber() + 1,
                notifications.getTotalPages());

        return ResponseEntity.ok(notifications);
    }

    /**
     * Get a specific notification by ID.
     * Requirement 6.2: WHEN an admin requests GET /api/notifications/{id} THEN
     * return the notification with the specified ID
     */
    @Operation(summary = "Get notification by ID", description = "Retrieve a specific notification by its ID. Requires ADMIN role or ownership of the notification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved notification", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Notification.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have permission to view this notification"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @notificationController.isOwner(#id, authentication)")
    public ResponseEntity<Notification> getNotificationById(
            @Parameter(description = "Notification ID", example = "1", required = true) @PathVariable Long id,
            Authentication authentication) {
        log.info("GET /api/notifications/{} - requested by: {}", id, getCurrentUserEmail());

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + id));

        log.info("Retrieved notification {} for destinataire: {}", id, notification.getDestinataire());

        return ResponseEntity.ok(notification);
    }

    /**
     * Get all notifications for a specific user email.
     * Requirement 6.3: WHEN a user requests GET /api/notifications/user/{email}
     * THEN return only notifications for that email
     */
    @Operation(summary = "Get notifications by user email", description = "Retrieve all notifications for a specific user. Users can only access their own notifications unless they have ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user notifications", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User cannot access another user's notifications")
    })
    @GetMapping("/user/{email}")
    @PreAuthorize("hasRole('ADMIN') or #email == authentication.principal")
    public ResponseEntity<Page<Notification>> getNotificationsByUser(
            @Parameter(description = "User email address", example = "doctorant@emsi.ma", required = true) @PathVariable String email,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/notifications/user/{} - page: {}, size: {}", email, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        Page<Notification> notifications = notificationHistoryService.getNotificationsByUser(email, pageable);

        log.info("Retrieved {} notifications for user: {}", notifications.getNumberOfElements(), email);

        return ResponseEntity.ok(notifications);
    }

    /**
     * Get unread notifications for a specific user by ID.
     * Returns notifications that have not been read yet.
     */
    @Operation(summary = "Get unread notifications by user ID", description = "Retrieve all unread notifications for a specific user by their ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved unread notifications", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token")
    })
    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("hasAnyRole('DOCTORANT', 'DIRECTEUR', 'ADMIN')")
    public ResponseEntity<List<Notification>> getUnreadNotificationsByUserId(
            @Parameter(description = "User ID", example = "7", required = true) @PathVariable Long userId) {

        log.info("GET /api/notifications/user/{}/unread", userId);

        // For now, return an empty list since we don't have user ID in notifications
        // In a real implementation, you would query by user ID or email
        List<Notification> notifications = notificationRepository.findByUserIdAndLuFalse(userId);

        log.info("Retrieved {} unread notifications for user ID: {}", notifications.size(), userId);

        return ResponseEntity.ok(notifications);
    }

    /**
     * Get all notifications with a specific status.
     * Requirement 6.4: WHEN an admin requests GET
     * /api/notifications/status/{status} THEN return all notifications with that
     * status
     */
    @Operation(summary = "Get notifications by status", description = "Retrieve all notifications with a specific status (PENDING, SENT, FAILED, RETRYING). Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved notifications by status", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Notification>> getNotificationsByStatus(
            @Parameter(description = "Notification status", example = "FAILED", required = true) @PathVariable StatutNotification status,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/notifications/status/{} - page: {}, size: {}", status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        Page<Notification> notifications = notificationHistoryService.getNotificationsByStatus(status, pageable);

        log.info("Retrieved {} notifications with status: {}", notifications.getNumberOfElements(), status);

        return ResponseEntity.ok(notifications);
    }

    /**
     * Get notification statistics.
     * Requirement 6.5: WHEN an admin requests GET /api/notifications/stats THEN
     * return statistics including total, sent, failed, pending counts and success
     * rate
     */
    @Operation(summary = "Get notification statistics", description = "Retrieve aggregated statistics about notification delivery including total, sent, failed, pending counts and success rate. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved notification statistics", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationStatsDTO.class), examples = @ExampleObject(name = "Statistics Example", value = "{\"total\": 1000, \"sent\": 950, \"failed\": 30, \"pending\": 10, \"retrying\": 10, \"successRate\": 96.94}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationStatsDTO> getNotificationStats() {
        log.info("GET /api/notifications/stats - requested by: {}", getCurrentUserEmail());

        NotificationStatsDTO stats = notificationHistoryService.getNotificationsStats();

        log.info("Notification stats retrieved - Total: {}, Sent: {}, Failed: {}, Success Rate: {}%",
                stats.getTotal(), stats.getSent(), stats.getFailed(), stats.getSuccessRate());

        return ResponseEntity.ok(stats);
    }

    /**
     * Retry a failed notification.
     * Requirement 6.6: WHEN an admin requests POST /api/notifications/{id}/retry
     * THEN retry sending the failed notification
     */
    @Operation(summary = "Retry a failed notification", description = "Retry sending a failed notification. The notification must have status FAILED. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification queued for retry", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Success Response", value = "{\"success\": true, \"message\": \"Notification queued for retry\", \"notificationId\": 123, \"status\": \"RETRYING\"}"))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Notification cannot be retried (not in FAILED status)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PostMapping("/{id}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retryNotification(
            @Parameter(description = "Notification ID to retry", example = "123", required = true) @PathVariable Long id) {
        log.info("POST /api/notifications/{}/retry - requested by: {}", id, getCurrentUserEmail());

        try {
            Notification notification = notificationHistoryService.retryFailedNotification(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Notification queued for retry");
            response.put("notificationId", id);
            response.put("status", notification.getStatut());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Notification not found: {}", id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalStateException e) {
            log.error("Cannot retry notification {}: {}", id, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get all failed notifications.
     * Requirement 6.7: WHEN an admin requests GET /api/notifications/failed THEN
     * return all notifications with status FAILED
     */
    @Operation(summary = "Get all failed notifications", description = "Retrieve all notifications with status FAILED. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved failed notifications", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    @GetMapping("/failed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Notification>> getFailedNotifications(
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/notifications/failed - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        Page<Notification> notifications = notificationHistoryService.getNotificationsByStatus(
                StatutNotification.FAILED,
                pageable);

        log.info("Retrieved {} failed notifications", notifications.getNumberOfElements());

        return ResponseEntity.ok(notifications);
    }

    /**
     * Search notifications with filters.
     * Requirement 6.8: WHEN an admin requests GET /api/notifications/search THEN
     * filter notifications by destinataire, type, status, and date range
     */
    @Operation(summary = "Search notifications with filters", description = "Search and filter notifications by recipient email, type, status, and date range. All filters are optional. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved filtered notifications", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role")
    })
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Notification>> searchNotifications(
            @Parameter(description = "Filter by recipient email", example = "doctorant@emsi.ma") @RequestParam(required = false) String destinataire,
            @Parameter(description = "Filter by notification type", example = "INSCRIPTION_VALIDEE_ADMIN") @RequestParam(required = false) TypeNotification type,
            @Parameter(description = "Filter by notification status", example = "SENT") @RequestParam(required = false) StatutNotification status,
            @Parameter(description = "Filter by start date (ISO format)", example = "2024-01-01T00:00:00") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @Parameter(description = "Filter by end date (ISO format)", example = "2024-12-31T23:59:59") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @Parameter(description = "Page number (0-indexed)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20") int size) {

        log.info(
                "GET /api/notifications/search - destinataire: {}, type: {}, status: {}, dateDebut: {}, dateFin: {}, page: {}, size: {}",
                destinataire, type, status, dateDebut, dateFin, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());
        Page<Notification> notifications = notificationRepository.searchNotifications(
                destinataire,
                type,
                status,
                dateDebut,
                dateFin,
                pageable);

        log.info("Search returned {} notifications", notifications.getNumberOfElements());

        return ResponseEntity.ok(notifications);
    }

    /**
     * Retry all messages from the Dead Letter Queue.
     * Requirement 11.4: WHEN an admin requests POST
     * /api/notifications/dlq/retry-all THEN reprocess all messages from the DLQ
     */
    @Operation(summary = "Retry all DLQ messages", description = "Reprocess all failed notifications from the Dead Letter Queue. This will attempt to resend all notifications that failed after exhausting retries. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "DLQ retry operation completed", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Success Response", value = "{\"success\": true, \"message\": \"DLQ retry operation completed\", \"totalProcessed\": 50, \"successCount\": 45, \"failureCount\": 5}"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have ADMIN role"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error - Error during DLQ retry operation")
    })
    @PostMapping("/dlq/retry-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> retryAllDLQMessages() {
        log.info("POST /api/notifications/dlq/retry-all - requested by: {}", getCurrentUserEmail());

        try {
            List<NotificationDLQ> dlqEntries = notificationDLQRepository.findByRetraiteOrderByDateAjoutDlqAsc(false);

            log.info("Found {} DLQ entries to reprocess", dlqEntries.size());

            int successCount = 0;
            int failureCount = 0;

            for (NotificationDLQ dlqEntry : dlqEntries) {
                try {
                    Notification notification = notificationHistoryService.retryFailedNotification(
                            dlqEntry.getNotificationId());

                    dlqEntry.setRetraite(true);
                    dlqEntry.setDateDerniereTentative(LocalDateTime.now());
                    notificationDLQRepository.save(dlqEntry);

                    successCount++;
                    log.info("Successfully queued DLQ entry {} (notification {}) for retry",
                            dlqEntry.getId(), dlqEntry.getNotificationId());

                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to reprocess DLQ entry {} (notification {}): {}",
                            dlqEntry.getId(), dlqEntry.getNotificationId(), e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "DLQ retry operation completed");
            response.put("totalProcessed", dlqEntries.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);

            log.info("DLQ retry-all completed - Total: {}, Success: {}, Failed: {}",
                    dlqEntries.size(), successCount, failureCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during DLQ retry-all operation: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error during DLQ retry operation: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Helper method to check if the current user is the owner of a notification.
     * Used for authorization in @PreAuthorize expressions.
     */
    public boolean isOwner(Long notificationId, Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return false;
        }

        String userEmail = authentication.getPrincipal().toString();

        return notificationRepository.findById(notificationId)
                .map(notification -> notification.getDestinataire().equals(userEmail))
                .orElse(false);
    }

    /**
     * Helper method to get the current user's email from the security context.
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            return authentication.getPrincipal().toString();
        }
        return "anonymous";
    }
}
