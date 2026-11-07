package com.example.incidentagent.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Each test rolls back, so the catalog starts empty every time. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class ServiceCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MonitoredServiceRepository repository;

    @Test
    void listReturnsAnEmptyArrayWhenThereAreNoServices() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listReturnsServicesSortedByName() throws Exception {
        repository.save(new MonitoredService("payments-worker", "Processes payments"));
        repository.save(new MonitoredService("cache-service", "Shared cache"));
        repository.save(new MonitoredService("orders-api", "Customer-facing API"));

        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("cache-service"))
                .andExpect(jsonPath("$[1].name").value("orders-api"))
                .andExpect(jsonPath("$[2].name").value("payments-worker"))
                .andExpect(jsonPath("$[0].description").value("Shared cache"))
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void getReturnsOneServiceByName() throws Exception {
        repository.save(new MonitoredService("orders-api", "Customer-facing API"));

        mockMvc.perform(get("/api/services/orders-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("orders-api"))
                .andExpect(jsonPath("$.description").value("Customer-facing API"));
    }

    @Test
    void getUnknownServiceReturnsTheStandardNotFoundEnvelope() throws Exception {
        mockMvc.perform(get("/api/services/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Service 'does-not-exist' not found"))
                .andExpect(jsonPath("$.path").value("/api/services/does-not-exist"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
