package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.Mention;

import java.time.LocalDateTime;

/**
 * DTO for finalizing a defense
 * Contains all information needed to complete a defense and generate the
 * procès-verbal
 * Implements Requirements 3.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinalizationDTO {

    @NotNull(message = "La mention est requise")
    private Mention mention;

    @NotNull(message = "La recommandation de publication est requise")
    private Boolean publicationRecommended;

    private String juryComments;

    @NotNull(message = "La date de délibération est requise")
    private LocalDateTime deliberationDate;
}
