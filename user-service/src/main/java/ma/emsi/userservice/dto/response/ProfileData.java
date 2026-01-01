package ma.emsi.userservice.dto.response;

import java.time.LocalDate;

public record ProfileData(
        LocalDate dateNaissance,
        String lieuNaissance,
        String nationalite,
        String cin,
        String photoUrl) {
}
