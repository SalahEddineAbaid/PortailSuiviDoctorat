package ma.emsi.userservice.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String type
) {
    public LoginResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer");
    }
}
