package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefenseRequestSummaryDTO {
    private Long id;
    private Long doctorantId;
    private String doctorantName;
    private DefenseRequestStatus status;
    private LocalDate doctorateStartDate;
    private long durationInYears;
    private String nextAction;
    private LocalDateTime submissionDate;
}
