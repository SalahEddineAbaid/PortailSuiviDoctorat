package ma.emsi.batchservice.writer;

import ma.emsi.batchservice.model.Token;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configuration class for password reset token deletion writer.
 * Executes DELETE statements in batches of 100 for optimal performance.
 * All deletions within a chunk are committed in a single transaction.
 * 
 * Validates: Requirements 1.4
 */
@Configuration
public class PasswordResetTokenDeletionWriterConfig {

    /**
     * Creates a JdbcBatchItemWriter for deleting password reset tokens.
     * 
     * @param userDataSource DataSource for userdb
     * @return Configured ItemWriter for password reset token deletion
     */
    @Bean
    public JdbcBatchItemWriter<Token> passwordResetTokenDeletionWriter(
            @Qualifier("userDataSource") DataSource userDataSource) {

        String sql = "DELETE FROM password_reset_token WHERE id = :id";

        return new JdbcBatchItemWriterBuilder<Token>()
                .dataSource(userDataSource)
                .sql(sql)
                .beanMapped()
                .build();
    }
}
