package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapportResponseDTO {

    private Long id;
    private String reportUrl;
    private boolean favorable;
    private LocalDateTime submissionDate;
    private Long defenseRequestId;
    private Long juryMemberId;
    private String juryMemberName; // Info enrichie
}
