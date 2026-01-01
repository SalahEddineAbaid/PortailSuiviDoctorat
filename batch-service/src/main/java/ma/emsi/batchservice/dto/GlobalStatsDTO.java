package ma.emsi.batchservice.dto;

/**
 * DTO for global batch job statistics.
 * Provides aggregate metrics across all jobs for monitoring and reporting.
 */
public class GlobalStatsDTO {
    private Long totalExecutions;
    private Long successfulExecutions;
    private Long failedExecutions;
    private Double successRate;
    private String averageDuration;
    private JobFailureDTO lastFailure;

    public GlobalStatsDTO() {
    }

    public GlobalStatsDTO(Long totalExecutions, Long successfulExecutions, Long failedExecutions,
            Double successRate, String averageDuration, JobFailureDTO lastFailure) {
        this.totalExecutions = totalExecutions;
        this.successfulExecutions = successfulExecutions;
        this.failedExecutions = failedExecutions;
        this.successRate = successRate;
        this.averageDuration = averageDuration;
        this.lastFailure = lastFailure;
    }

    public Long getTotalExecutions() {
        return totalExecutions;
    }

    public void setTotalExecutions(Long totalExecutions) {
        this.totalExecutions = totalExecutions;
    }

    public Long getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public void setSuccessfulExecutions(Long successfulExecutions) {
        this.successfulExecutions = successfulExecutions;
    }

    public Long getFailedExecutions() {
        return failedExecutions;
    }

    public void setFailedExecutions(Long failedExecutions) {
        this.failedExecutions = failedExecutions;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public String getAverageDuration() {
        return averageDuration;
    }

    public void setAverageDuration(String averageDuration) {
        this.averageDuration = averageDuration;
    }

    public JobFailureDTO getLastFailure() {
        return lastFailure;
    }

    public void setLastFailure(JobFailureDTO lastFailure) {
        this.lastFailure = lastFailure;
    }
}
