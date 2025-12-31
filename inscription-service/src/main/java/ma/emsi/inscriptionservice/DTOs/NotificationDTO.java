package ma.emsi.inscriptionservice.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String destinataireEmail;
    private String destinataireNom;
    private String sujet;
    private String message;
    private TypeNotification type;
    private Long inscriptionId;
    private LocalDateTime dateEnvoi;

    public enum TypeNotification {
        NOUVELLE_DEMANDE_DIRECTEUR,
        NOUVELLE_DEMANDE_ADMIN,
        VALIDATION_DIRECTEUR,
        REJET_DIRECTEUR,
        VALIDATION_ADMIN,
        REJET_ADMIN,
        VALIDATION_DEFINITIVE,
        RAPPEL_DOCUMENTS,
        DEROGATION_DEMANDEE,
        DEROGATION_APPROUVEE_DIRECTEUR,
        DEROGATION_REJETEE,
        DEROGATION_APPROUVEE,
        CAMPAGNE_OUVERTE,
        CAMPAGNE_FERMEE
    }
}
