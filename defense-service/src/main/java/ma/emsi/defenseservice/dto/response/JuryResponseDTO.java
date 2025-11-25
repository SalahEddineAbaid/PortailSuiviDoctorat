package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.JuryStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JuryResponseDTO {

    private Long id;
    private Long directorId;
    private JuryStatus status;
    private LocalDateTime proposalDate;
    private LocalDateTime validationDate;
    private Long defenseRequestId;

    // Liste des membres
    private List<JuryMemberResponseDTO> members;
}
