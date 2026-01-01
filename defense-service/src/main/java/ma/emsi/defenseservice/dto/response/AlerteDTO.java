package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlerteDTO {
    private String type;
    private String message;
    private String severity; // INFO, WARNING, CRITICAL
    private Long relatedEntityId;
    private LocalDateTime timestamp;
}
