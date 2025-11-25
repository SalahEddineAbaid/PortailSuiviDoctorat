package ma.emsi.defenseservice.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO pour représenter un utilisateur du user-service
 * Doit correspondre exactement à UserResponse du user-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;

    @JsonProperty("FirstName")
    private String firstName;

    @JsonProperty("LastName")
    private String lastName;

    private String phoneNumber;
    private String adresse;
    private String ville;
    private String pays;
    private Set<String> roles;
}
