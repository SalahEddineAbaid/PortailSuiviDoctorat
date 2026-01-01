package ma.emsi.userservice.controller;

import lombok.RequiredArgsConstructor;
import ma.emsi.userservice.dto.response.AuditRecordResponse;
import ma.emsi.userservice.enums.AuditAction;
import ma.emsi.userservice.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for audit trail operations.
 * All endpoints require ADMIN role.
 * Requirements: 10.4
 */
@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /**
     * Get audit history for a specific user with optional action filtering.
     * GET /api/admin/audit/users/{userId}
     * Requirements: 7.3
     *
     * @param userId the user ID to query
     * @param action optional action type filter (null for all actions)
     * @param page   page number (default 0)
     * @param size   page size (default 20)
     * @return paginated audit records
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<AuditRecordResponse>> getUserAuditHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Create pageable
        Pageable pageable = PageRequest.of(page, size);

        // Call auditService.getUserAuditHistory
        Page<AuditRecordResponse> auditHistory = auditService.getUserAuditHistory(
                userId, action, pageable);

        // Return 200 OK with paginated audit records
        return ResponseEntity.ok(auditHistory);
    }

    /**
     * Get recent audit records from the last 24 hours.
     * GET /api/admin/audit/recent
     * Requirements: 7.4
     *
     * @return list of recent audit records
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AuditRecordResponse>> getRecentAudits() {

        // Call auditService.getRecentAudits
        List<AuditRecordResponse> recentAudits = auditService.getRecentAudits();

        // Return 200 OK with recent audits
        return ResponseEntity.ok(recentAudits);
    }
}
