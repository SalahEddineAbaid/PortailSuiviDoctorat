package ma.emsi.batchservice.dto;

import java.time.LocalDateTime;

/**
 * DTO for job trigger response.
 * Returned when a job is manually triggered via REST API.
 */
public class JobTriggerResponseDTO {
    private Long executionId;
    private String status;
    private LocalDateTime startTime;
    private String message;

    public JobTriggerResponseDTO() {
    }

    public JobTriggerResponseDTO(Long executionId, String status, LocalDateTime startTime, String message) {
        this.executionId = executionId;
        this.status = status;
        this.startTime = startTime;
        this.message = message;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
