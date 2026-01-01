package ma.emsi.batchservice.reader;

import ma.emsi.batchservice.model.Token;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Configuration class for expired password reset token reader.
 * Reads tokens where expiry_date < current timestamp.
 * Configured with fetch size of 100 for optimal performance.
 * 
 * Validates: Requirements 1.3
 */
@Configuration
public class ExpiredPasswordResetTokenReaderConfig {

    /**
     * Creates a JdbcCursorItemReader for expired password reset tokens.
     * 
     * @param userDataSource DataSource for userdb
     * @return Configured ItemReader for expired password reset tokens
     */
    @Bean
    public JdbcCursorItemReader<Token> expiredPasswordResetTokenReader(
            @Qualifier("userDataSource") DataSource userDataSource) {

        String sql = "SELECT id, token, expiry_date FROM password_reset_token WHERE expiry_date < NOW()";

        return new JdbcCursorItemReaderBuilder<Token>()
                .name("expiredPasswordResetTokenReader")
                .dataSource(userDataSource)
                .sql(sql)
                .rowMapper(new PasswordResetTokenRowMapper())
                .fetchSize(100)
                .build();
    }

    /**
     * RowMapper for mapping password reset token result set to Token model.
     */
    private static class PasswordResetTokenRowMapper implements RowMapper<Token> {
        @Override
        public Token mapRow(ResultSet rs, int rowNum) throws SQLException {
            Token token = new Token();
            token.setId(rs.getLong("id"));
            token.setToken(rs.getString("token"));
            token.setExpiryDate(rs.getTimestamp("expiry_date").toLocalDateTime());
            token.setType("PASSWORD_RESET");
            return token;
        }
    }
}
