package ma.emsi.userservice.dto.response;

import ma.emsi.userservice.enums.AccountStatus;

import java.util.Set;

public record UserDetailedResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String adresse,
        String ville,
        String pays,
        Set<String> roles,
        boolean profileComplete,
        ProfileData profile,
        AccountStatus accountStatus) {
}
