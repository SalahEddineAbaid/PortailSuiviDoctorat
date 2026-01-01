package ma.emsi.batchservice.dto;

import java.time.LocalDateTime;

/**
 * DTO for job failure information.
 * Contains details about the last job failure for monitoring purposes.
 */
public class JobFailureDTO {
    private String jobName;
    private LocalDateTime failureTime;
    private String exitMessage;

    public JobFailureDTO() {
    }

    public JobFailureDTO(String jobName, LocalDateTime failureTime, String exitMessage) {
        this.jobName = jobName;
        this.failureTime = failureTime;
        this.exitMessage = exitMessage;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public LocalDateTime getFailureTime() {
        return failureTime;
    }

    public void setFailureTime(LocalDateTime failureTime) {
        this.failureTime = failureTime;
    }

    public String getExitMessage() {
        return exitMessage;
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
    }
}
