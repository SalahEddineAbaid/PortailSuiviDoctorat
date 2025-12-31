# Swagger/OpenAPI Documentation

## Overview

The Notification Service provides comprehensive API documentation using SpringDoc OpenAPI (Swagger). This documentation is automatically generated from the code annotations and provides an interactive interface for testing the API endpoints.

## Accessing Swagger UI

Once the notification-service is running, you can access the Swagger UI at:

**URL**: http://localhost:8084/swagger-ui.html

Alternative access points:
- **OpenAPI JSON**: http://localhost:8084/api-docs
- **OpenAPI YAML**: http://localhost:8084/api-docs.yaml

## Features

### Interactive API Documentation

The Swagger UI provides:

1. **Complete API Reference**: All REST endpoints with detailed descriptions
2. **Request/Response Examples**: Sample payloads for all operations
3. **Try It Out**: Interactive testing of endpoints directly from the browser
4. **Schema Definitions**: Complete data models with field descriptions
5. **Authentication Support**: JWT Bearer token authentication

### API Endpoints Documented

The following endpoint groups are documented:

#### Notification Management
- `GET /api/notifications` - Get all notifications (paginated)
- `GET /api/notifications/{id}` - Get notification by ID
- `GET /api/notifications/user/{email}` - Get user's notifications
- `GET /api/notifications/status/{status}` - Get notifications by status
- `GET /api/notifications/stats` - Get notification statistics
- `GET /api/notifications/failed` - Get all failed notifications
- `GET /api/notifications/search` - Search notifications with filters
- `POST /api/notifications/{id}/retry` - Retry a failed notification
- `POST /api/notifications/dlq/retry-all` - Retry all DLQ messages

### Data Models

The following DTOs and entities are fully documented:

1. **NotificationDTO**: Request payload for creating notifications
2. **NotificationStatsDTO**: Statistics response with aggregated metrics
3. **Notification**: Complete notification entity with all fields
4. **Enums**: TypeNotification, StatutNotification, PrioriteNotification

## Using Swagger UI

### 1. Authentication

Most endpoints require JWT authentication:

1. Click the **Authorize** button at the top right
2. Enter your JWT token in the format: `Bearer <your-token>`
3. Click **Authorize** to save the token
4. The token will be automatically included in all subsequent requests

### 2. Testing Endpoints

To test an endpoint:

1. Expand the endpoint you want to test
2. Click **Try it out**
3. Fill in the required parameters
4. Click **Execute**
5. View the response below, including status code, headers, and body

### 3. Viewing Examples

Each endpoint includes:
- **Request examples**: Sample payloads you can use
- **Response examples**: Expected response formats
- **Parameter descriptions**: Detailed information about each parameter

## Configuration

The Swagger UI is configured in `application.properties`:

```properties
# SpringDoc OpenAPI (Swagger) Configuration
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true
springdoc.show-actuator=false
```

### Customization Options

You can customize the Swagger UI by modifying these properties:

- `springdoc.swagger-ui.operationsSorter`: Sort operations by method or alpha
- `springdoc.swagger-ui.tagsSorter`: Sort tags alphabetically
- `springdoc.swagger-ui.tryItOutEnabled`: Enable/disable the "Try it out" feature
- `springdoc.show-actuator`: Show/hide actuator endpoints

## API Information

The API documentation includes:

- **Title**: Notification Service API
- **Version**: 1.0.0
- **Description**: Microservice for managing email notifications in the doctoral management system
- **Contact**: EMSI Development Team (support@emsi.ma)
- **License**: Apache 2.0

## Security Scheme

The API uses JWT Bearer authentication:

- **Type**: HTTP
- **Scheme**: bearer
- **Bearer Format**: JWT
- **Description**: Enter JWT token obtained from the authentication service

## Testing with Swagger

### Example: Get Notification Statistics

1. Navigate to http://localhost:8084/swagger-ui.html
2. Find the `GET /api/notifications/stats` endpoint
3. Click **Try it out**
4. Click **Execute**
5. View the response with statistics like:
   ```json
   {
     "total": 1000,
     "sent": 950,
     "failed": 30,
     "pending": 10,
     "retrying": 10,
     "successRate": 96.94
   }
   ```

### Example: Search Notifications

1. Find the `GET /api/notifications/search` endpoint
2. Click **Try it out**
3. Fill in optional filters:
   - `destinataire`: doctorant@emsi.ma
   - `status`: SENT
   - `dateDebut`: 2024-01-01T00:00:00
   - `dateFin`: 2024-12-31T23:59:59
4. Click **Execute**
5. View the filtered results

## Troubleshooting

### Swagger UI Not Loading

If Swagger UI doesn't load:

1. Verify the service is running: http://localhost:8084/actuator/health
2. Check the OpenAPI JSON is accessible: http://localhost:8084/api-docs
3. Review application logs for errors
4. Ensure SpringDoc dependency is in pom.xml

### Authentication Issues

If you get 401 Unauthorized errors:

1. Ensure you've clicked the **Authorize** button
2. Verify your JWT token is valid and not expired
3. Check that the token includes the required roles (ADMIN for most endpoints)
4. Ensure the token format is: `Bearer <token>` (not just the token)

### CORS Issues

If testing from a different origin:

1. Check CORS configuration in SecurityConfig
2. Ensure the frontend URL is whitelisted
3. Verify preflight OPTIONS requests are allowed

## Production Considerations

### Disabling Swagger in Production

To disable Swagger UI in production, set:

```properties
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

Or use Spring profiles:

```properties
# application-prod.properties
springdoc.swagger-ui.enabled=false
```

### Security

- Always require authentication for Swagger UI in production
- Consider restricting access to specific IP addresses
- Use HTTPS in production environments
- Regularly update SpringDoc dependency for security patches

## Additional Resources

- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Specification](https://swagger.io/specification/)
- [Swagger UI Documentation](https://swagger.io/tools/swagger-ui/)

## Support

For issues or questions about the API documentation:
- Contact: support@emsi.ma
- Review the API design document: `.kiro/specs/notification-service-finalisation/design.md`
- Check the requirements document: `.kiro/specs/notification-service-finalisation/requirements.md`
