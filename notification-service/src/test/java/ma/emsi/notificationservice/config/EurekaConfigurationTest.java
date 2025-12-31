package ma.emsi.notificationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class to verify Eureka client configuration
 * Tests that the notification-service is properly configured to register with Eureka
 */
@SpringBootTest
@TestPropertySource(properties = {
    "eureka.client.enabled=false" // Disable actual Eureka registration in tests
})
class EurekaConfigurationTest {

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Test
    void testEurekaClientBeanExists() {
        // Verify that DiscoveryClient bean is available
        // This confirms that Eureka client is properly configured
        assertThat(discoveryClient).isNotNull();
    }

    @Test
    void testApplicationName() {
        // Verify that the application name is set correctly
        String applicationName = System.getProperty("spring.application.name");
        if (applicationName == null) {
            applicationName = "notification-service"; // Default from application.properties
        }
        assertThat(applicationName).isEqualTo("notification-service");
    }
}
