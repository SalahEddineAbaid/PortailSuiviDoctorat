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
 * Configuration class for expired refresh token reader.
 * Reads tokens where expiry_date < current timestamp.
 * Configured with fetch size of 100 for optimal performance.
 * 
 * Validates: Requirements 1.2
 */
@Configuration
public class ExpiredRefreshTokenReaderConfig {

    /**
     * Creates a JdbcCursorItemReader for expired refresh tokens.
     * 
     * @param userDataSource DataSource for userdb
     * @return Configured ItemReader for expired refresh tokens
     */
    @Bean
    public JdbcCursorItemReader<Token> expiredRefreshTokenReader(
            @Qualifier("userDataSource") DataSource userDataSource) {

        String sql = "SELECT id, token, expiry_date FROM refresh_token WHERE expiry_date < NOW()";

        return new JdbcCursorItemReaderBuilder<Token>()
                .name("expiredRefreshTokenReader")
                .dataSource(userDataSource)
                .sql(sql)
                .rowMapper(new RefreshTokenRowMapper())
                .fetchSize(100)
                .build();
    }

    /**
     * RowMapper for mapping refresh token result set to Token model.
     */
    private static class RefreshTokenRowMapper implements RowMapper<Token> {
        @Override
        public Token mapRow(ResultSet rs, int rowNum) throws SQLException {
            Token token = new Token();
            token.setId(rs.getLong("id"));
            token.setToken(rs.getString("token"));
            token.setExpiryDate(rs.getTimestamp("expiry_date").toLocalDateTime());
            token.setType("REFRESH");
            return token;
        }
    }
}
