package ma.emsi.defenseservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.defenseservice.enums.DocumentType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDTO {

    private Long id;
    private String fileName;
    private String fileType;
    private String fileUrl;
    private DocumentType type;
    private LocalDateTime uploadDate;
}
