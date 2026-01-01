package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefenseScheduledDTO {
    private Long defenseId;
    private Long defenseRequestId;
    private Long doctorantId;
    private String doctorantName;
    private LocalDateTime defenseDate;
    private String location;
    private String room;
}
