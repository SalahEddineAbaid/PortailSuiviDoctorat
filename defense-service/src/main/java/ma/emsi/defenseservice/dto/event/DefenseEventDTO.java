package ma.emsi.defenseservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefenseEventDTO {
    private String eventType;
    private LocalDateTime timestamp;
    private Map<String, Object> payload;
}
