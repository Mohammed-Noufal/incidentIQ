package com.example.incidentagent.catalog;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Dev convenience, enabled with APP_DEV_DATA=true: inserts illustrative services if the table is empty.
 * The names are placeholders; final names come from the approved demo script.
 */
@Component
@ConditionalOnProperty(name = "app.dev-data.enabled", havingValue = "true")
public class DevServiceDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevServiceDataLoader.class);

    private final MonitoredServiceRepository repository;

    public DevServiceDataLoader(MonitoredServiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new MonitoredService("payments-worker", "Processes payments; depends on the database connection pool"),
                new MonitoredService("orders-api", "Customer-facing API; database-backed"),
                new MonitoredService("cache-service", "Shared cache in front of the database")));
        log.info("Dev data: inserted 3 illustrative services");
    }
}
