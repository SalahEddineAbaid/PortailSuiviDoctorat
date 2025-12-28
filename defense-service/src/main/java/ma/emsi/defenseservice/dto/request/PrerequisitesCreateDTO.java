package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrerequisitesCreateDTO {

    @NotNull(message = "L'ID du doctorant est requis")
    private Long doctorantId;

    @Min(value = 0, message = "Le nombre d'articles doit être positif")
    private int journalArticles;

    @Min(value = 0, message = "Le nombre de conférences doit être positif")
    private int conferences;

    @Min(value = 0, message = "Le nombre d'heures doit être positif")
    private int trainingHours;

    private boolean manuscriptUploaded;
    private boolean antiPlagiarismUploaded;
    private boolean publicationsReportUploaded;
    private boolean trainingCertsUploaded;
    private boolean authorizationLetterUploaded;
}
