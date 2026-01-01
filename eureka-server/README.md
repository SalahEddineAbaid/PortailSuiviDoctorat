# Eureka Server - Service Discovery

A highly available, secure Eureka Server implementation with advanced monitoring and configuration optimizations.

## 🎯 Features

✅ **High Availability** - Peer-to-peer replication with 2-node cluster support  
✅ **Security** - Basic Authentication for dashboard and API endpoints  
✅ **Monitoring** - Spring Boot Actuator with Prometheus metrics  
✅ **Advanced Configuration** - Optimized timeouts, caching, and eviction settings  
✅ **Health Checks** - Custom health indicators for cluster monitoring

---

## 📋 Architecture

```
┌─────────────────┐         ┌─────────────────┐
│  Eureka Peer 1  │◄───────►│  Eureka Peer 2  │
│   Port: 8761    │  Sync   │   Port: 8762    │
└────────┬────────┘         └────────┬────────┘
         │                           │
         └───────────┬───────────────┘
                     │
            ┌────────▼────────┐
            │  Microservices  │
            │  Registration   │
            └─────────────────┘
```

---

## 🚀 Quick Start

### Standalone Mode (Single Instance)

```bash
# Build the project
mvn clean package

# Run standalone
mvn spring-boot:run
```

Access dashboard: **http://localhost:8761**  
Credentials: `eureka` / `eureka123`

### Cluster Mode (High Availability)

**Terminal 1 - Start Peer 1:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=peer1
```

**Terminal 2 - Start Peer 2:**
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=peer2
```

Access dashboards:
- **Peer 1**: http://localhost:8761 (credentials: eureka/eureka123)
- **Peer 2**: http://localhost:8762 (credentials: eureka/eureka123)

---

## 🔐 Security Configuration

### Default Credentials

| Username | Password   |
|----------|------------|
| eureka   | eureka123  |

### Microservice Registration

All microservices must include credentials in their Eureka client configuration:

**application.properties:**
```properties
eureka.client.service-url.defaultZone=http://eureka:eureka123@localhost:8761/eureka/,http://eureka:eureka123@localhost:8762/eureka/
```

**application.yml:**
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka:eureka123@localhost:8761/eureka/,http://eureka:eureka123@localhost:8762/eureka/
```

> [!WARNING]
> **Change default credentials before deploying to production!**

---

## 📊 Monitoring Endpoints

All endpoints require authentication (eureka/eureka123):

| Endpoint                              | Description                          |
|---------------------------------------|--------------------------------------|
| `/actuator/health`                    | Health status with Eureka details    |
| `/actuator/info`                      | Application information              |
| `/actuator/metrics`                   | Available metrics list               |
| `/actuator/prometheus`                | Prometheus-formatted metrics         |

### Example Health Check

```bash
curl -u eureka:eureka123 http://localhost:8761/actuator/health
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "eureka": {
      "status": "UP",
      "details": {
        "registeredInstances": 5,
        "upInstances": 5,
        "downInstances": 0,
        "numberOfPeers": 1,
        "renewsLastMin": 12,
        "renewThreshold": 10,
        "selfPreservationMode": "OFF"
      }
    }
  }
}
```

---

## ⚙️ Configuration Profiles

### Standalone (default)

- **Port**: 8761
- **Self-registration**: Disabled
- **Registry fetch**: Disabled
- **Best for**: Development, testing

### Peer 1 & Peer 2 (cluster)

- **Ports**: 8761, 8762
- **Self-registration**: Enabled with peer
- **Replication**: Bidirectional sync
- **Best for**: Production, high availability

---

## 🔧 Advanced Configuration

### Lease & Renewal Settings

```properties
# How often clients send heartbeats (30 seconds)
eureka.instance.lease-renewal-interval-in-seconds=30

# Time before evicting non-renewing instances (90 seconds)
eureka.instance.lease-expiration-duration-in-seconds=90

# Registry fetch interval for clients (30 seconds)
eureka.client.registry-fetch-interval-seconds=30
```

### Self-Preservation Mode

```properties
# Enable self-preservation mode
eureka.server.enable-self-preservation=true

# Threshold for triggering self-preservation (85%)
eureka.server.self-preservation-mode-threshold=0.85
```

### Response Cache

```properties
# Cache update interval (30 seconds)
eureka.server.response-cache-update-interval-ms=30000

# Cache expiration time (3 minutes)
eureka.server.response-cache-auto-expiration-in-seconds=180
```

### Rate Limiting

```properties
eureka.server.rate-limiter-enabled=true
eureka.server.rate-limiter-burst-size=10
eureka.server.rate-limiter-registry-fetch-average-rate=500
```

---

## 🧪 Verification & Testing

### 1. Test Standalone Instance

```bash
# Start server
mvn spring-boot:run

# Verify health
curl -u eureka:eureka123 http://localhost:8761/actuator/health

# Access dashboard (use browser)
# URL: http://localhost:8761
# Login: eureka / eureka123
```

### 2. Test Cluster Replication

```bash
# Terminal 1: Start peer1
mvn spring-boot:run -Dspring-boot.run.profiles=peer1

# Terminal 2: Start peer2
mvn spring-boot:run -Dspring-boot.run.profiles=peer2

# Verify peer1 sees peer2 as replica
curl -u eureka:eureka123 http://localhost:8761/eureka/apps

# Verify peer2 sees peer1 as replica
curl -u eureka:eureka123 http://localhost:8762/eureka/apps
```

### 3. Test Failover

1. Start both peers
2. Register a test service with peer1
3. Verify service appears in peer2 dashboard
4. Stop peer1
5. Confirm peer2 continues operating with registered services

---

## 📦 Dependencies

| Dependency                          | Purpose                              |
|-------------------------------------|--------------------------------------|
| `spring-cloud-netflix-eureka-server`| Core Eureka Server functionality     |
| `spring-boot-starter-security`      | Dashboard and API authentication     |
| `spring-boot-starter-actuator`      | Health checks and monitoring         |
| `micrometer-registry-prometheus`    | Prometheus metrics export            |

---

## 🚨 Troubleshooting

### Dashboard shows "EMERGENCY! EUREKA MAY BE INCORRECTLY CLAIMING INSTANCES ARE UP"

**Cause**: Self-preservation mode activated due to low renewal rate.

**Solution**: This is normal during development when few services are registered. In production, ensure:
- Sufficient services are registered
- Network connectivity is stable
- Lease renewal intervals are appropriate

### Peer replication not working

**Verify**:
1. Both peers are running on correct ports (8761, 8762)
2. Credentials are correct in peer URLs
3. No firewall blocking peer communication
4. Check logs for replication errors

### Cannot access dashboard

**Verify**:
1. Correct URL: http://localhost:8761
2. Using credentials: eureka / eureka123
3. Server is running (check console logs)
4. No port conflicts

---

## 🏭 Production Recommendations

> [!IMPORTANT]
> **Before deploying to production:**

1. **Change default credentials** - Update in `application.properties`
2. **Use HTTPS** - Configure SSL certificates
3. **Deploy on separate machines** - Update hostnames in peer configurations
4. **Externalize configuration** - Use Spring Cloud Config Server
5. **Set up monitoring** - Integrate with Prometheus/Grafana
6. **Configure load balancer** - Front Eureka instances for single entry point
7. **Adjust timeouts** - Based on your infrastructure and requirements

### Production Profile Example

```properties
# application-production.properties
spring.security.user.name=${EUREKA_USERNAME}
spring.security.user.password=${EUREKA_PASSWORD}

eureka.instance.hostname=${EUREKA_HOSTNAME}
eureka.client.service-url.defaultZone=${EUREKA_PEERS}

# Enable HTTPS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
```

---

## 📝 Version Information

- **Version**: 1.0.0
- **Spring Boot**: 3.5.6
- **Spring Cloud**: 2025.0.0
- **Eureka Server**: 4.3.0
- **Java**: 17

---

## 📖 Additional Resources

- [Spring Cloud Netflix Documentation](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/)
- [Eureka Wiki](https://github.com/Netflix/eureka/wiki)
- [Spring Boot Actuator Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus Integration](https://prometheus.io/docs/introduction/overview/)
