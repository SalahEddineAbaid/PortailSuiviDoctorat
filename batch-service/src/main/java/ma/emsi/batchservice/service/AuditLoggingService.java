package ma.emsi.batchservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.emsi.batchservice.entity.AuditLog;
import ma.emsi.batchservice.repository.AuditLogRepository;
import org.springframework.batch.core.JobExecution;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Logging Service
 * Provides immutable audit logging for all batch operations
 * Requirements: 10.6, 10.7, 10.8, 10.9
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log job execution start
     * Requirement: 10.7
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logJobExecutionStart(String jobName, Long executionId, boolean isManual) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("JOB_EXECUTION")
                    .eventAction("START")
                    .jobName(jobName)
                    .executionId(executionId)
                    .status("IN_PROGRESS")
                    .details(isManual ? "Manual trigger" : "Scheduled execution")
                    .build();

            if (isManual) {
                enrichWithUserContext(auditLog);
            }

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for job execution start: {}", jobName);
        } catch (Exception e) {
            log.error("Failed to create audit log for job execution start: {}", e.getMessage(), e);
        }
    }

    /**
     * Log job execution completion
     * Requirement: 10.7
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logJobExecutionComplete(JobExecution jobExecution) {
        try {
            String status = jobExecution.getStatus().isUnsuccessful() ? "FAILURE" : "SUCCESS";

            AuditLog auditLog = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("JOB_EXECUTION")
                    .eventAction("COMPLETE")
                    .jobName(jobExecution.getJobInstance().getJobName())
                    .executionId(jobExecution.getId())
                    .status(status)
                    .details(String.format("Exit status: %s, Exit description: %s",
                            jobExecution.getExitStatus().getExitCode(),
                            jobExecution.getExitStatus().getExitDescription()))
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for job execution complete: {}", jobExecution.getJobInstance().getJobName());
        } catch (Exception e) {
            log.error("Failed to create audit log for job execution complete: {}", e.getMessage(), e);
        }
    }

    /**
     * Log manual job trigger
     * Requirement: 10.8
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logManualJobTrigger(String jobName, Long executionId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("MANUAL_TRIGGER")
                    .eventAction("TRIGGER")
                    .jobName(jobName)
                    .executionId(executionId)
                    .status("SUCCESS")
                    .details("Job manually triggered via REST API")
                    .build();

            enrichWithUserContext(auditLog);
            enrichWithRequestContext(auditLog);

            auditLogRepository.save(auditLog);
            log.info("Audit log created for manual job trigger: {} by user: {}", jobName, auditLog.getUserId());
        } catch (Exception e) {
            log.error("Failed to create audit log for manual job trigger: {}", e.getMessage(), e);
        }
    }

    /**
     * Log archive file access
     * Requirement: 10.6
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logArchiveAccess(String filePath, String action) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("ARCHIVE_ACCESS")
                    .eventAction(action)
                    .resourcePath(filePath)
                    .status("SUCCESS")
                    .details(String.format("Archive file accessed: %s", action))
                    .build();

            enrichWithUserContext(auditLog);
            enrichWithRequestContext(auditLog);

            auditLogRepository.save(auditLog);
            log.info("Audit log created for archive access: {} by user: {}", filePath, auditLog.getUserId());
        } catch (Exception e) {
            log.error("Failed to create audit log for archive access: {}", e.getMessage(), e);
        }
    }

    /**
     * Log failed archive access attempt
     * Requirement: 10.6
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logArchiveAccessFailure(String filePath, String reason) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType("ARCHIVE_ACCESS")
                    .eventAction("ACCESS_DENIED")
                    .resourcePath(filePath)
                    .status("FAILURE")
                    .details(String.format("Archive access denied: %s", reason))
                    .build();

            enrichWithUserContext(auditLog);
            enrichWithRequestContext(auditLog);

            auditLogRepository.save(auditLog);
            log.warn("Audit log created for failed archive access: {} by user: {}", filePath, auditLog.getUserId());
        } catch (Exception e) {
            log.error("Failed to create audit log for archive access failure: {}", e.getMessage(), e);
        }
    }

    /**
     * Log general security event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityEvent(String eventType, String action, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .eventType(eventType)
                    .eventAction(action)
                    .status("INFO")
                    .details(details)
                    .build();

            enrichWithUserContext(auditLog);
            enrichWithRequestContext(auditLog);

            auditLogRepository.save(auditLog);
            log.debug("Audit log created for security event: {} - {}", eventType, action);
        } catch (Exception e) {
            log.error("Failed to create audit log for security event: {}", e.getMessage(), e);
        }
    }

    /**
     * Retrieve audit logs by event type
     */
    public List<AuditLog> getAuditLogsByEventType(String eventType) {
        return auditLogRepository.findByEventTypeOrderByTimestampDesc(eventType);
    }

    /**
     * Retrieve audit logs by user
     */
    public List<AuditLog> getAuditLogsByUser(String userId) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Retrieve audit logs by job name
     */
    public List<AuditLog> getAuditLogsByJobName(String jobName) {
        return auditLogRepository.findByJobNameOrderByTimestampDesc(jobName);
    }

    /**
     * Retrieve recent audit logs
     */
    public List<AuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop100ByOrderByTimestampDesc();
    }

    /**
     * Retrieve audit logs within time range
     */
    public List<AuditLog> getAuditLogsByTimeRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    /**
     * Enrich audit log with user context from Spring Security
     */
    private void enrichWithUserContext(AuditLog auditLog) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                auditLog.setUserId(authentication.getName());

                if (authentication.getAuthorities() != null && !authentication.getAuthorities().isEmpty()) {
                    String roles = authentication.getAuthorities().stream()
                            .map(Object::toString)
                            .reduce((a, b) -> a + "," + b)
                            .orElse("");
                    auditLog.setUserRole(roles);
                }
            }
        } catch (Exception e) {
            log.debug("Could not enrich audit log with user context: {}", e.getMessage());
        }
    }

    /**
     * Enrich audit log with HTTP request context
     */
    private void enrichWithRequestContext(AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.debug("Could not enrich audit log with request context: {}", e.getMessage());
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs in X-Forwarded-For
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}
