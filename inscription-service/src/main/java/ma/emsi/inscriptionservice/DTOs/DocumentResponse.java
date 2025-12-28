package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.TypeDocument;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private TypeDocument typeDocument;
    private String nomFichier;
    private Long tailleFichier;
    private String mimeType;
    private LocalDateTime dateUpload;
    private Boolean valide;
    private String commentaire;
}
