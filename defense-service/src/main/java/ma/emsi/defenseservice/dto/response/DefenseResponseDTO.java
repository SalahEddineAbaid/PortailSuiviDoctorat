package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.DefenseStatus;
import ma.emsi.defenseservice.enums.Mention;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefenseResponseDTO {

    private Long id;
    private LocalDateTime defenseDate;
    private String location;
    private String room;
    private DefenseStatus status;
    private String procesVerbalUrl;
    private Mention mention;
    private boolean publicationRecommended;
    private String juryComments;
    private LocalDateTime deliberationDate;
    private Long defenseRequestId;
}
