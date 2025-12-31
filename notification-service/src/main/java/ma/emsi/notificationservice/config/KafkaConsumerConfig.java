package ma.emsi.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import ma.emsi.notificationservice.dtos.NotificationDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for notification service.
 * Configures JSON deserialization with trusted packages and error handling.
 * 
 * Requirements: 1.1 - Configure Kafka consumer with JSON deserialization
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;
    
    /**
     * Creates the consumer factory with JSON deserialization configuration.
     * Configures trusted packages to allow deserialization of NotificationDTO.
     * Uses ErrorHandlingDeserializer to gracefully handle deserialization failures.
     * 
     * @return configured consumer factory
     */
    @Bean
    public ConsumerFactory<String, NotificationDTO> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        
        // Key deserializer
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Value deserializer with error handling wrapper
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        
        // JSON deserializer configuration - trusted packages set to "*" to allow all
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, NotificationDTO.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        return new DefaultKafkaConsumerFactory<>(
            config,
            new StringDeserializer(),
            new JsonDeserializer<>(NotificationDTO.class, false)
        );
    }
    
    /**
     * Creates the Kafka listener container factory.
     * Configures acknowledgment mode, concurrency, and error handling.
     * 
     * @return configured container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationDTO> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationDTO> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        
        // Set acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        
        // Set concurrency (number of consumer threads)
        factory.setConcurrency(3);
        
        // Enable batch listening if needed (currently disabled for single message processing)
        factory.setBatchListener(false);
        
        // Configure error handler for deserialization and processing errors
        factory.setCommonErrorHandler(kafkaErrorHandler());
        
        return factory;
    }
    
    /**
     * Creates an error handler for Kafka listener errors.
     * Handles deserialization failures and other exceptions gracefully.
     * 
     * @return configured error handler
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        // Configure backoff: no retries at the container level
        // (retries are handled by Resilience4j in the EmailService)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (consumerRecord, exception) -> {
                // Log deserialization errors
                log.error("Error processing Kafka message at offset {}: {}", 
                         consumerRecord.offset(), exception.getMessage());
                log.error("Failed record: {}", consumerRecord);
                
                // In production, you might want to:
                // 1. Send to a separate error topic
                // 2. Store in database for manual review
                // 3. Send alerts to operations team
            },
            new FixedBackOff(0L, 0L) // No retries at container level
        );
        
        // Don't retry on deserialization errors
        errorHandler.addNotRetryableExceptions(
            org.springframework.kafka.support.serializer.DeserializationException.class
        );
        
        return errorHandler;
    }
}
