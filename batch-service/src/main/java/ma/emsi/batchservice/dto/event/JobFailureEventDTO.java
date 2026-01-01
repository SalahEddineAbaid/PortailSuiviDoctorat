package ma.emsi.batchservice.dto.event;

import java.time.LocalDateTime;

/**
 * DTO for job failure events published to Kafka.
 * 
 * Contains detailed information about job failures for alerting and monitoring.
 * 
 * Requirements: 8.8, 8.9
 */
public class JobFailureEventDTO {

    private String type; // JOB_FAILURE, STEP_FAILURE
    private String jobName;
    private Long executionId;
    private LocalDateTime failureTime;
    private String exitCode;
    private String exitMessage;
    private String exceptionType;
    private String exceptionMessage;
    private String exceptionClass;
    private String stackTrace;
    private String priority; // NORMAL, HIGH, URGENT
    private String notificationType; // JOB_FAILURE, STEP_FAILURE
    private Boolean restartable;
    private String suggestedAction;

    public JobFailureEventDTO() {
    }

    // Getters and Setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public LocalDateTime getFailureTime() {
        return failureTime;
    }

    public void setFailureTime(LocalDateTime failureTime) {
        this.failureTime = failureTime;
    }

    public String getExitCode() {
        return exitCode;
    }

    public void setExitCode(String exitCode) {
        this.exitCode = exitCode;
    }

    public String getExitMessage() {
        return exitMessage;
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public Boolean getRestartable() {
        return restartable;
    }

    public void setRestartable(Boolean restartable) {
        this.restartable = restartable;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    @Override
    public String toString() {
        return "JobFailureEventDTO{" +
                "type='" + type + '\'' +
                ", jobName='" + jobName + '\'' +
                ", executionId=" + executionId +
                ", failureTime=" + failureTime +
                ", exceptionType='" + exceptionType + '\'' +
                ", exceptionMessage='" + exceptionMessage + '\'' +
                ", priority='" + priority + '\'' +
                ", notificationType='" + notificationType + '\'' +
                ", restartable=" + restartable +
                '}';
    }
}
