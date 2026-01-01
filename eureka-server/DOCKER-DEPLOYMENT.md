# Docker Deployment Guide - Eureka Server Cluster

## 🐳 Quick Start with Docker Compose

### Prerequisites
- Docker installed (version 20.10+)
- Docker Compose installed (version 2.0+)

---

## 🚀 Deployment Steps

### 1. Build the Application

```bash
cd c:\Users\hp\Desktop\PortailSuiviDoctorat\eureka-server

# Build the JAR file
.\mvnw.cmd clean package -DskipTests
```

### 2. Build Docker Images

```bash
# Build images for both Eureka instances
docker-compose build
```

### 3. Start the Cluster

```bash
# Start both Eureka servers
docker-compose up -d

# View logs
docker-compose logs -f
```

### 4. Verify Deployment

**Check running containers:**
```bash
docker-compose ps
```

Expected output:
```
NAME            IMAGE                    STATUS        PORTS
eureka-peer1    eureka-server:latest     Up (healthy)  0.0.0.0:8761->8761/tcp
eureka-peer2    eureka-server:latest     Up (healthy)  0.0.0.0:8762->8762/tcp
```

**Access dashboards:**
- Peer 1: http://localhost:8761 (credentials: eureka/eureka123)
- Peer 2: http://localhost:8762 (credentials: eureka/eureka123)

---

## 🛠️ Common Commands

### Start the cluster
```bash
docker-compose up -d
```

### Stop the cluster
```bash
docker-compose down
```

### Restart the cluster
```bash
docker-compose restart
```

### View logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f eureka-peer1
docker-compose logs -f eureka-peer2
```

### Check health status
```bash
docker-compose ps

# Detailed health check
docker inspect eureka-peer1 --format='{{.State.Health.Status}}'
```

### Rebuild after changes
```bash
# Rebuild application
.\mvnw.cmd clean package -DskipTests

# Rebuild and restart containers
docker-compose up -d --build
```

---

## 🔍 Troubleshooting

### Issue: Container not starting

**Check logs:**
```bash
docker-compose logs eureka-peer1
docker-compose logs eureka-peer2
```

**Check if JAR exists:**
```bash
ls target/*.jar
```

**Solution:** Rebuild the application first
```bash
.\mvnw.cmd clean package -DskipTests
docker-compose up -d --build
```

### Issue: Peers not synchronizing

**Symptoms:** One peer doesn't show the other in "available-replicas"

**Check network:**
```bash
docker network inspect eureka-cluster-network
```

**Restart both services:**
```bash
docker-compose restart
```

### Issue: Health check failing

**Check health endpoint manually:**
```bash
curl -u eureka:eureka123 http://localhost:8761/actuator/health
curl -u eureka:eureka123 http://localhost:8762/actuator/health
```

**View health check logs:**
```bash
docker inspect eureka-peer1 --format='{{json .State.Health}}' | jq
```

---

## 🔐 Security Notes

### Default Credentials
- Username: `eureka`
- Password: `eureka123`

### Production Deployment

**Change credentials using environment variables:**

```yaml
environment:
  - SPRING_SECURITY_USER_NAME=${EUREKA_USERNAME}
  - SPRING_SECURITY_USER_PASSWORD=${EUREKA_PASSWORD}
```

**Create a `.env` file:**
```bash
EUREKA_USERNAME=your_username
EUREKA_PASSWORD=your_secure_password
```

**Load environment variables:**
```bash
docker-compose --env-file .env up -d
```

---

## 📊 Monitoring

### Container Stats
```bash
docker stats eureka-peer1 eureka-peer2
```

### Health Checks
```bash
# Peer 1
curl -u eureka:eureka123 http://localhost:8761/actuator/health

# Peer 2
curl -u eureka:eureka123 http://localhost:8762/actuator/health
```

### Prometheus Metrics
```bash
# Peer 1 metrics
curl -u eureka:eureka123 http://localhost:8761/actuator/prometheus

# Peer 2 metrics
curl -u eureka:eureka123 http://localhost:8762/actuator/prometheus
```

---

## 🌐 Network Configuration

### Custom Network

The Docker Compose creates a dedicated bridge network: `eureka-cluster-network`

**Connect other microservices:**

```yaml
services:
  your-service:
    networks:
      - eureka-cluster-network
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:eureka123@eureka-peer1:8761/eureka/,http://eureka:eureka123@eureka-peer2:8762/eureka/

networks:
  eureka-cluster-network:
    external: true
```

---

## 🔄 Update Strategy

### Zero-Downtime Update

1. **Update peer2 first:**
```bash
docker-compose stop eureka-peer2
.\mvnw.cmd clean package -DskipTests
docker-compose up -d --build eureka-peer2
```

2. **Wait for peer2 to be healthy:**
```bash
docker-compose ps eureka-peer2
```

3. **Update peer1:**
```bash
docker-compose stop eureka-peer1
docker-compose up -d --build eureka-peer1
```

---

## 📦 Resource Limits (Optional)

Add to `docker-compose.yml` for production:

```yaml
services:
  eureka-peer1:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

---

## 🎯 Integration with Microservices

### For Docker-based Microservices

Update their `docker-compose.yml`:

```yaml
services:
  your-service:
    environment:
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka:eureka123@eureka-peer1:8761/eureka/,http://eureka:eureka123@eureka-peer2:8762/eureka/
    networks:
      - eureka-cluster-network
    depends_on:
      eureka-peer1:
        condition: service_healthy

networks:
  eureka-cluster-network:
    external: true
```

---

## 📝 Summary

Docker Compose simplifies Eureka cluster deployment:
- ✅ Automatic networking between peers
- ✅ Health checks and automatic restarts
- ✅ Easy scaling and updates
- ✅ Consistent environment across deployments

**Start command:**
```bash
docker-compose up -d
```

**Access:**
- http://localhost:8761 (peer1)
- http://localhost:8762 (peer2)

**Credentials:** eureka / eureka123
