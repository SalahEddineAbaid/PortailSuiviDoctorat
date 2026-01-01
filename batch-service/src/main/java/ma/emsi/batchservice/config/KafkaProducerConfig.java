package ma.emsi.batchservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration for batch-service.
 * Configures ProducerFactory with JSON serialization for publishing events to
 * Kafka topics.
 * Implements retry logic and idempotence for reliable event delivery.
 * 
 * Used for:
 * - Publishing duration alert events to notifications topic
 * - Publishing job failure notifications
 * - Publishing monthly report notifications
 * 
 * Requirements: 1.6, 2.3, 2.5, 2.6
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Creates ProducerFactory with JSON serialization configuration.
     * Configures retry logic and idempotence for reliable message delivery.
     * 
     * Configuration:
     * - Key serializer: StringSerializer (for doctorant_id, job_name, etc.)
     * - Value serializer: JsonSerializer (for event DTOs)
     * - Acks: all (ensures message is written to all replicas)
     * - Retries: 3 (automatic retry on transient failures)
     * - Idempotence: enabled (prevents duplicate messages on retry)
     * 
     * @return ProducerFactory configured for JSON serialization
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Bootstrap servers configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serialization configuration
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability configuration
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Performance tuning
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        // Compression for better network utilization
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Timeout configuration
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates KafkaTemplate for sending messages to Kafka topics.
     * This template is used by ItemWriters and Tasklets to publish events.
     * 
     * Usage examples:
     * - kafkaTemplate.send("notifications", doctorantId, alertEventDTO)
     * - kafkaTemplate.send("notifications", jobName, jobFailureEventDTO)
     * 
     * @return KafkaTemplate configured with ProducerFactory
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
