package ma.emsi.userservice.dto.response;

import ma.emsi.userservice.enums.AuditAction;

import java.time.LocalDateTime;

/**
 * Response DTO for audit record information.
 * Contains all audit trail data for a specific user action.
 */
public record AuditRecordResponse(
        Long id,
        Long userId,
        AuditAction action,
        LocalDateTime timestamp,
        String ipAddress,
        String details) {
}
