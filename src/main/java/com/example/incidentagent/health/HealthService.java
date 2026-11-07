package com.example.incidentagent.health;

import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Reports dependency status so a failing dependency is visible before a demo.
 * MySQL is actually pinged. Hindsight and Groq are only reported as configured or not (no network call,
 * and the keys are never exposed). Real connectivity checks arrive with their clients in Step 2.
 */
@Service
public class HealthService {

    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
    public static final String CONFIGURED = "CONFIGURED";
    public static final String NOT_CONFIGURED = "NOT_CONFIGURED";

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final DataSource dataSource;
    private final boolean groqConfigured;
    private final boolean hindsightConfigured;

    public HealthService(DataSource dataSource,
                         @Value("${app.groq.api-key:}") String groqApiKey,
                         @Value("${app.hindsight.api-key:}") String hindsightApiKey) {
        this.dataSource = dataSource;
        this.groqConfigured = StringUtils.hasText(groqApiKey);
        this.hindsightConfigured = StringUtils.hasText(hindsightApiKey);
    }

    public HealthResponse check() {
        ComponentHealth mysql = checkDatabase();
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("mysql", mysql);
        components.put("hindsight", ComponentHealth.configured(hindsightConfigured));
        components.put("groq", ComponentHealth.configured(groqConfigured));
        String overall = UP.equals(mysql.status()) ? UP : DOWN;
        return new HealthResponse(overall, Instant.now(), components);
    }

    private ComponentHealth checkDatabase() {
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return ComponentHealth.up((System.nanoTime() - start) / 1_000_000);
            }
            return ComponentHealth.down("Database connection is not valid");
        } catch (Exception ex) {
            log.warn("Database health check failed: {}", ex.toString());
            return ComponentHealth.down("Database unreachable");
        }
    }
}
