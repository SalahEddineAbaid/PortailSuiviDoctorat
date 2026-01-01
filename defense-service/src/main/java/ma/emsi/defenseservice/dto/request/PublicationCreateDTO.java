package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.QuartileJournal;
import ma.emsi.defenseservice.enums.TypePublication;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicationCreateDTO {

    @NotNull(message = "L'ID des prérequis est requis")
    private Long prerequisitesId;

    @NotBlank(message = "Le titre est requis")
    private String titre;

    private String journal;

    private Integer anneePublication;

    @NotNull(message = "Le type de publication est requis")
    private TypePublication type;

    private QuartileJournal quartile;

    private String doi;

    private String url;
}
