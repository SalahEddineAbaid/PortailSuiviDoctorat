package ma.emsi.inscriptionservice.DTOs;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;

    @JsonProperty("FirstName")
    @JsonAlias({ "firstName", "first_name" })
    private String firstName;

    @JsonProperty("LastName")
    @JsonAlias({ "lastName", "last_name" })
    private String lastName;

    private String email;
    private String phoneNumber;
    private String adresse;
    private String ville;
    private String pays;
    private Set<String> roles;
    private Boolean active;

    // Helper method to get first role
    public String getRole() {
        if (roles != null && !roles.isEmpty()) {
            return roles.iterator().next();
        }
        return null;
    }
}
