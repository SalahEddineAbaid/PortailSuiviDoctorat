package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrerequisitesResponseDTO {

    private Long id;
    private Long doctorantId;
    private int journalArticles;
    private int conferences;
    private int trainingHours;
    private boolean manuscriptUploaded;
    private boolean antiPlagiarismUploaded;
    private boolean publicationsReportUploaded;
    private boolean trainingCertsUploaded;
    private boolean authorizationLetterUploaded;
    private boolean isValid;
}
