FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Copy the jar file
COPY target/*.jar eureka-server.jar

# Expose Eureka ports
EXPOSE 8761 8762

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://eureka:eureka123@localhost:8761/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "eureka-server.jar"]
