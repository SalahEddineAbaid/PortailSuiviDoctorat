package ma.emsi.defenseservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.DocumentType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadDTO {

    @NotBlank(message = "Le nom du fichier est requis")
    private String fileName;

    @NotBlank(message = "Le type de fichier est requis")
    private String fileType;

    @NotBlank(message = "L'URL du fichier est requise")
    private String fileUrl;

    @NotNull(message = "Le type de document est requis")
    private DocumentType type;

    @NotNull(message = "L'ID de la demande est requis")
    private Long defenseRequestId;
}
