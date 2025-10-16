package ma.emsi.userservice.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
