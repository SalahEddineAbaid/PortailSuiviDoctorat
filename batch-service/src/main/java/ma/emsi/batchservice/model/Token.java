package ma.emsi.batchservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Model representing a token (refresh token or password reset token) from
 * userdb.
 * Used by token cleanup job to identify and delete expired tokens.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    private Long id;
    private String token;
    private LocalDateTime expiryDate;
    private String type; // "REFRESH" or "PASSWORD_RESET"
}
