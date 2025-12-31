package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.inscriptionservice.enums.TypeDocument;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentManquant {
    private TypeDocument type;
    private String libelle;
    private Boolean obligatoire;
}
