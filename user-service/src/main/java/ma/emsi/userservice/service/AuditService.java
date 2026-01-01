package ma.emsi.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.userservice.dto.response.AuditRecordResponse;
import ma.emsi.userservice.entity.UserAudit;
import ma.emsi.userservice.enums.AuditAction;
import ma.emsi.userservice.repository.UserAuditRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing audit trail records.
 * Logs all security-sensitive actions and provides query capabilities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final UserAuditRepository auditRepository;

    /**
     * Log a login attempt (success or failure).
     *
     * @param userId    the user ID
     * @param ipAddress the IP address of the login attempt
     * @param success   true if login was successful, false otherwise
     */
    @Transactional
    public void logLogin(Long userId, String ipAddress, boolean success) {
        AuditAction action = success ? AuditAction.LOGIN : AuditAction.LOGIN_FAILED;
        String details = success ? "Successful login" : "Failed login attempt";

        createAuditRecord(userId, action, ipAddress, details);

        log.info("Logged {} for user {} from IP {}", action, userId, ipAddress);
    }

    /**
     * Log a password change action.
     *
     * @param userId    the user ID
     * @param ipAddress the IP address of the request
     */
    @Transactional
    public void logPasswordChange(Long userId, String ipAddress) {
        createAuditRecord(userId, AuditAction.PASSWORD_CHANGE, ipAddress,
                "User changed their password");

        log.info("Logged password change for user {} from IP {}", userId, ipAddress);
    }

    /**
     * Log a role assignment action.
     *
     * @param userId  the user ID who received the role
     * @param role    the role that was assigned
     * @param adminId the admin user ID who performed the action
     */
    @Transactional
    public void logRoleAssigned(Long userId, String role, Long adminId) {
        String details = String.format("Role '%s' assigned by admin ID %d", role, adminId);

        createAuditRecord(userId, AuditAction.ROLE_ASSIGNED, null, details);

        log.info("Logged role assignment: {} assigned to user {} by admin {}",
                role, userId, adminId);
    }

    /**
     * Log a profile modification action.
     *
     * @param userId    the user ID
     * @param ipAddress the IP address of the request
     */
    @Transactional
    public void logProfileModified(Long userId, String ipAddress) {
        createAuditRecord(userId, AuditAction.PROFILE_MODIFIED, ipAddress,
                "User profile information updated");

        log.info("Logged profile modification for user {} from IP {}", userId, ipAddress);
    }

    /**
     * Log an account disabled action.
     *
     * @param userId  the user ID whose account was disabled
     * @param reason  the reason for disabling the account
     * @param adminId the admin user ID who performed the action
     */
    @Transactional
    public void logAccountDisabled(Long userId, String reason, Long adminId) {
        String details = String.format("Account disabled by admin ID %d. Reason: %s",
                adminId, reason);

        createAuditRecord(userId, AuditAction.ACCOUNT_DISABLED, null, details);

        log.info("Logged account disabled for user {} by admin {}", userId, adminId);
    }

    /**
     * Log an account enabled action.
     *
     * @param userId  the user ID whose account was enabled
     * @param adminId the admin user ID who performed the action
     */
    @Transactional
    public void logAccountEnabled(Long userId, Long adminId) {
        String details = String.format("Account enabled by admin ID %d", adminId);

        createAuditRecord(userId, AuditAction.ACCOUNT_ENABLED, null, details);

        log.info("Logged account enabled for user {} by admin {}", userId, adminId);
    }

    /**
     * Log an account locked action due to failed login attempts.
     *
     * @param userId    the user ID whose account was locked
     * @param ipAddress the IP address of the last failed attempt
     * @param attempts  the number of failed attempts that triggered the lock
     */
    @Transactional
    public void logAccountLocked(Long userId, String ipAddress, int attempts) {
        String details = String.format("Account locked after %d failed login attempts", attempts);

        createAuditRecord(userId, AuditAction.ACCOUNT_LOCKED, ipAddress, details);

        log.info("Logged account locked for user {} after {} failed attempts from IP {}",
                userId, attempts, ipAddress);
    }

    /**
     * Get audit history for a specific user with optional action filtering.
     *
     * @param userId   the user ID to query
     * @param action   optional action type filter (null for all actions)
     * @param pageable pagination information
     * @return paginated audit records
     */
    @Transactional(readOnly = true)
    public Page<AuditRecordResponse> getUserAuditHistory(Long userId, AuditAction action,
            Pageable pageable) {
        Page<UserAudit> auditPage;

        if (action != null) {
            auditPage = auditRepository.findByUserIdAndActionOrderByTimestampDesc(
                    userId, action, pageable);
        } else {
            auditPage = auditRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        }

        return auditPage.map(this::mapToResponse);
    }

    /**
     * Get recent audit records from the last 24 hours.
     *
     * @return list of recent audit records
     */
    @Transactional(readOnly = true)
    public List<AuditRecordResponse> getRecentAudits() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        List<UserAudit> recentAudits = auditRepository.findByTimestampAfter(twentyFourHoursAgo);

        return recentAudits.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create and persist an audit record.
     *
     * @param userId    the user ID
     * @param action    the audit action type
     * @param ipAddress the IP address (can be null)
     * @param details   additional details about the action
     */
    private void createAuditRecord(Long userId, AuditAction action, String ipAddress,
            String details) {
        UserAudit audit = UserAudit.builder()
                .userId(userId)
                .action(action)
                .timestamp(LocalDateTime.now())
                .ipAddress(ipAddress)
                .details(details)
                .build();

        auditRepository.save(audit);
    }

    /**
     * Map UserAudit entity to AuditRecordResponse DTO.
     *
     * @param audit the audit entity
     * @return the response DTO
     */
    private AuditRecordResponse mapToResponse(UserAudit audit) {
        return new AuditRecordResponse(
                audit.getId(),
                audit.getUserId(),
                audit.getAction(),
                audit.getTimestamp(),
                audit.getIpAddress(),
                audit.getDetails());
    }
}
