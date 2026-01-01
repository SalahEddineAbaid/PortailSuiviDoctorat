package ma.emsi.batchservice.dto;

import java.time.LocalDateTime;

/**
 * DTO for job execution information returned by REST API.
 * Contains complete execution details including status, timing, and metrics.
 */
public class JobExecutionDTO {
    private Long executionId;
    private String jobName;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String duration; // Formatted as "5m 23s"
    private Integer itemsProcessed;
    private Integer itemsFailed;
    private String exitMessage;

    public JobExecutionDTO() {
    }

    public JobExecutionDTO(Long executionId, String jobName, String status, LocalDateTime startTime,
            LocalDateTime endTime, String duration, Integer itemsProcessed,
            Integer itemsFailed, String exitMessage) {
        this.executionId = executionId;
        this.jobName = jobName;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.itemsProcessed = itemsProcessed;
        this.itemsFailed = itemsFailed;
        this.exitMessage = exitMessage;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
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

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Integer getItemsProcessed() {
        return itemsProcessed;
    }

    public void setItemsProcessed(Integer itemsProcessed) {
        this.itemsProcessed = itemsProcessed;
    }

    public Integer getItemsFailed() {
        return itemsFailed;
    }

    public void setItemsFailed(Integer itemsFailed) {
        this.itemsFailed = itemsFailed;
    }

    public String getExitMessage() {
        return exitMessage;
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
    }
}
