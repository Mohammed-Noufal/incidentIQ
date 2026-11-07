package com.example.incidentagent.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.incidentagent.catalog.MonitoredService;
import com.example.incidentagent.catalog.MonitoredServiceRepository;
import com.example.incidentagent.incident.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        serviceRepository.save(new MonitoredService("payments-worker", "Processes payments"));
    }

    @Test
    void createAlertReturns201AndCreatesIncidentAndAlert() throws Exception {
        String json = """
                {
                  "serviceName": "payments-worker",
                  "severity": "HIGH",
                  "title": "Checkout latency above 8s",
                  "message": "p95 checkout latency 8.4s for 6 minutes; error rate 12%",
                  "logExcerpt": "HikariPool-1 - Connection is not available"
                }
                """;

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertId").isNumber())
                .andExpect(jsonPath("$.incident.code").value("INC-001"))
                .andExpect(jsonPath("$.incident.status").value("OPEN"))
                .andExpect(jsonPath("$.incident.service").value("payments-worker"))
                .andExpect(jsonPath("$.incident.severity").value("HIGH"))
                .andExpect(jsonPath("$.incident.openedAt").exists());

        assertThat(incidentRepository.findAll()).hasSize(1);
        assertThat(alertRepository.findAll()).hasSize(1);
    }

    @Test
    void createAlertWithUnknownServiceReturns404() throws Exception {
        String json = """
                {
                  "serviceName": "unknown-service",
                  "severity": "HIGH",
                  "title": "Some alert",
                  "message": "Some message"
                }
                """;

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Service 'unknown-service' not found"));
    }

    @Test
    void createAlertWithMissingRequiredFieldsReturns422() throws Exception {
        String json = """
                {
                  "serviceName": "",
                  "title": "",
                  "message": ""
                }
                """;

        mockMvc.perform(post("/api/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());
    }
}
