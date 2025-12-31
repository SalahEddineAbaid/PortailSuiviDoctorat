package ma.emsi.notificationservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for notification statistics.
 * Provides aggregated metrics about notification delivery.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Notification statistics data transfer object")
public class NotificationStatsDTO {
    
    @Schema(description = "Total number of notifications", example = "1000")
    private Long total;
    
    @Schema(description = "Number of successfully sent notifications", example = "950")
    private Long sent;
    
    @Schema(description = "Number of failed notifications", example = "30")
    private Long failed;
    
    @Schema(description = "Number of pending notifications", example = "10")
    private Long pending;
    
    @Schema(description = "Number of notifications being retried", example = "10")
    private Long retrying;
    
    @Schema(description = "Success rate percentage (sent / (sent + failed) * 100)", example = "96.94")
    private Double successRate;
    
    /**
     * Calculate success rate based on sent and total notifications.
     * Success rate = (sent / (sent + failed)) * 100
     * Returns 0.0 if no notifications have been processed.
     */
    public void calculateSuccessRate() {
        if (sent == null || failed == null) {
            this.successRate = 0.0;
            return;
        }
        
        long processed = sent + failed;
        if (processed == 0) {
            this.successRate = 0.0;
        } else {
            this.successRate = (sent.doubleValue() / processed) * 100.0;
        }
    }
}
