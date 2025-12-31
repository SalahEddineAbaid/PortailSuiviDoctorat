package ma.emsi.notificationservice.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ma.emsi.notificationservice.enums.PrioriteNotification;
import ma.emsi.notificationservice.enums.TypeNotification;

import java.util.Map;

/**
 * Data Transfer Object for notification requests.
 * Used for Kafka message deserialization and REST API requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Notification request data transfer object")
public class NotificationDTO {
    
    @NotNull(message = "Type is required")
    @JsonProperty("type")
    @Schema(
        description = "Type of notification to send",
        example = "INSCRIPTION_VALIDEE_ADMIN",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private TypeNotification type;
    
    @NotBlank(message = "Destinataire is required")
    @Email(message = "Destinataire must be a valid email address")
    @JsonProperty("destinataire")
    @Schema(
        description = "Recipient email address",
        example = "doctorant@emsi.ma",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String destinataire;
    
    @NotBlank(message = "Sujet is required")
    @JsonProperty("sujet")
    @Schema(
        description = "Email subject line",
        example = "Votre inscription a été validée",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String sujet;
    
    @JsonProperty("messageTexte")
    @Schema(
        description = "Plain text version of the message",
        example = "Votre inscription au doctorat a été validée par l'administration."
    )
    private String messageTexte;
    
    @JsonProperty("priorite")
    @Builder.Default
    @Schema(
        description = "Priority level of the notification",
        example = "NORMALE",
        defaultValue = "NORMALE"
    )
    private PrioriteNotification priorite = PrioriteNotification.NORMALE;
    
    @JsonProperty("donnees")
    @Schema(
        description = "Dynamic data for template variable interpolation",
        example = "{\"nomDoctorant\": \"Ahmed Benali\", \"anneeUniversitaire\": \"2024-2025\", \"dateValidation\": \"2024-01-15\"}"
    )
    private Map<String, Object> donnees;
}
