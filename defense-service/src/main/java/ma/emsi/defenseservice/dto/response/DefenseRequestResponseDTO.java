package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.DefenseRequestStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefenseRequestResponseDTO {

    private Long id;
    private Long doctorantId;

    // ✅ Informations enrichies du doctorant (depuis user-service)
    private String doctorantFirstName;
    private String doctorantLastName;
    private String doctorantEmail;

    private LocalDateTime submissionDate;
    private DefenseRequestStatus status;
    private String rejectionReason;

    // Informations simplifiées des relations
    private Long prerequisitesId;
    private Long juryId;
    private Long defenseId;
    private int documentsCount;
    private int rapportsCount;
}
