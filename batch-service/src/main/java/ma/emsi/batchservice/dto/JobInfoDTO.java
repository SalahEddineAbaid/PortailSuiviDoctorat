package ma.emsi.batchservice.dto;

import java.time.LocalDateTime;

/**
 * DTO for job information including metadata and scheduling details.
 * Used for listing available jobs with their current status.
 */
public class JobInfoDTO {
    private String name;
    private String description;
    private String cronExpression;
    private LocalDateTime lastExecution;
    private String lastStatus;
    private Boolean isRunning;

    public JobInfoDTO() {
    }

    public JobInfoDTO(String name, String description, String cronExpression,
            LocalDateTime lastExecution, String lastStatus, Boolean isRunning) {
        this.name = name;
        this.description = description;
        this.cronExpression = cronExpression;
        this.lastExecution = lastExecution;
        this.lastStatus = lastStatus;
        this.isRunning = isRunning;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public LocalDateTime getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(LocalDateTime lastExecution) {
        this.lastExecution = lastExecution;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public void setLastStatus(String lastStatus) {
        this.lastStatus = lastStatus;
    }

    public Boolean getIsRunning() {
        return isRunning;
    }

    public void setIsRunning(Boolean isRunning) {
        this.isRunning = isRunning;
    }
}
