package ma.emsi.userservice.dto.response;

import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String FirstName,
        String LastName,
        String phoneNumber,
        String adresse,
        String ville,
        String pays,
        Set<String> roles
) {}
