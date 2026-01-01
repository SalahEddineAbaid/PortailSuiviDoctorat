package com.devbuild.eurekaserver.health;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom health indicator for Eureka Server
 * Provides detailed health information about registered instances and peer
 * replication
 */
@Component
public class EurekaHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();

            if (serverContext == null) {
                return Health.down()
                        .withDetail("reason", "Eureka server context not initialized")
                        .build();
            }

            PeerAwareInstanceRegistry registry = serverContext.getRegistry();
            Map<String, Object> details = new HashMap<>();

            // Count total registered instances
            int totalInstances = 0;
            int upInstances = 0;

            for (Application app : registry.getSortedApplications()) {
                for (InstanceInfo info : app.getInstances()) {
                    totalInstances++;
                    if (info.getStatus() == InstanceInfo.InstanceStatus.UP) {
                        upInstances++;
                    }
                }
            }

            details.put("registeredInstances", totalInstances);
            details.put("upInstances", upInstances);
            details.put("downInstances", totalInstances - upInstances);

            // Get renewal stats
            details.put("renewsLastMin", registry.getNumOfRenewsInLastMin());
            details.put("renewThreshold", registry.getNumOfRenewsPerMinThreshold());

            // Determine overall health status
            Health.Builder healthBuilder = Health.up();

            // If self-preservation mode is triggered, show warning
            if (registry.getNumOfRenewsInLastMin() < registry.getNumOfRenewsPerMinThreshold()) {
                healthBuilder = Health.status("WARNING");
                details.put("selfPreservationMode", "ENABLED - Renewals below threshold");
            } else {
                details.put("selfPreservationMode", "OFF");
            }

            return healthBuilder.withDetails(details).build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
