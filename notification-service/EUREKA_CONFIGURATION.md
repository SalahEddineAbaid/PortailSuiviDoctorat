# Eureka Client Configuration

## Overview

The notification-service is now configured to register with Eureka Server for service discovery. This allows other microservices in the system to discover and communicate with the notification-service dynamically.

## Configuration Details

### 1. Dependency Added

The Eureka client dependency is already included in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### 2. Application Annotation

The main application class has been updated with `@EnableDiscoveryClient`:

```java
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
```

### 3. Application Properties Configuration

The following Eureka configuration has been added to `application.properties`:

```properties
# ===================================
# Eureka Client Configuration
# ===================================
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true
eureka.instance.instance-id=${spring.application.name}:${spring.application.instance_id:${random.value}}
```

**Configuration Explanation:**

- `eureka.client.service-url.defaultZone`: The URL of the Eureka Server where the service will register
- `eureka.instance.prefer-ip-address=true`: Registers the service using its IP address instead of hostname
- `eureka.instance.instance-id`: Unique identifier for this service instance (includes application name and random value)

## Testing Service Registration

### Prerequisites

1. **Start Eureka Server** (on port 8761):
   ```bash
   cd eureka-server
   ./mvnw.cmd spring-boot:run
   ```

2. **Verify Eureka Server is running**:
   - Open browser: http://localhost:8761
   - You should see the Eureka dashboard

### Start Notification Service

1. **Start the notification-service**:
   ```bash
   cd notification-service
   ./mvnw.cmd spring-boot:run
   ```

2. **Verify Registration**:
   - Open browser: http://localhost:8761
   - Look for "NOTIFICATION-SERVICE" in the "Instances currently registered with Eureka" section
   - You should see the service listed with status "UP"

### Expected Output in Logs

When the notification-service starts, you should see logs similar to:

```
DiscoveryClient_NOTIFICATION-SERVICE - registration status: 204
DiscoveryClient_NOTIFICATION-SERVICE - Re-registering apps/NOTIFICATION-SERVICE
```

### Verify Service Discovery

You can verify the service is registered by calling the Eureka REST API:

```bash
curl http://localhost:8761/eureka/apps/NOTIFICATION-SERVICE
```

This should return XML with the service instance details.

## Configuration for Different Environments

### Development Environment

The current configuration uses `localhost:8761` which is suitable for local development.

### Production Environment

For production, update the Eureka URL in `application.properties` or use environment variables:

```properties
eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
```

Then set the environment variable:
```bash
export EUREKA_SERVER_URL=http://eureka-server:8761/eureka/
```

## Troubleshooting

### Service Not Registering

1. **Check Eureka Server is running**: Verify http://localhost:8761 is accessible
2. **Check network connectivity**: Ensure the notification-service can reach the Eureka Server
3. **Check logs**: Look for connection errors in the notification-service logs
4. **Verify configuration**: Ensure `eureka.client.service-url.defaultZone` is correct

### Service Shows as DOWN

1. **Check health endpoint**: Verify http://localhost:8084/actuator/health returns UP
2. **Check firewall**: Ensure port 8084 is not blocked
3. **Review Eureka configuration**: Verify `eureka.instance.prefer-ip-address` setting

## Integration with Other Services

Once registered with Eureka, other services can discover the notification-service using:

```java
@Autowired
private DiscoveryClient discoveryClient;

public String getNotificationServiceUrl() {
    List<ServiceInstance> instances = discoveryClient.getInstances("notification-service");
    if (!instances.isEmpty()) {
        return instances.get(0).getUri().toString();
    }
    throw new RuntimeException("notification-service not found");
}
```

Or use Feign clients with service discovery:

```java
@FeignClient(name = "notification-service")
public interface NotificationServiceClient {
    @GetMapping("/api/notifications")
    List<NotificationDTO> getAllNotifications();
}
```

## Requirements Validation

This implementation satisfies **Requirement 15.7**:
- ✅ Eureka client dependency added
- ✅ `eureka.client.service-url.defaultZone` configured
- ✅ `eureka.instance.prefer-ip-address` configured
- ✅ Service registration can be tested with Eureka Server
- ✅ Application annotated with `@EnableDiscoveryClient`

## Next Steps

1. Start the Eureka Server
2. Start the notification-service
3. Verify registration in the Eureka dashboard
4. Test service discovery from other microservices
