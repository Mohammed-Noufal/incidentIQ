package com.example.incidentagent.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.incidentagent.catalog.MonitoredService;
import com.example.incidentagent.catalog.MonitoredServiceRepository;
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
class IncidentLifecycleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @BeforeEach
    void setUp() {
        serviceRepository.save(new MonitoredService("payments-worker", "Processes payments"));
        serviceRepository.save(new MonitoredService("cache-service", "Cache"));
    }

    private void createIncident(String code) {
        MonitoredService s = serviceRepository.findByName("payments-worker").orElseThrow();
        Incident inc = new Incident();
        inc.setCode(code);
        inc.setService(s);
        inc.setTitle("Elevated payment latency");
        inc.setSymptoms("p95 latency > 8s");
        inc.setSeverity(Severity.HIGH);
        incidentRepository.save(inc);
    }

    @Test
    void actionRecordingAndOutcomeValidationFlow() throws Exception {
        createIncident("INC-019");

        // 1. Record an action attempt
        String actionJson = """
                {
                  "actionDescription": "Restarted cache-service",
                  "targetService": "cache-service"
                }
                """;

        mockMvc.perform(post("/api/incidents/INC-019/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentCode").value("INC-019"))
                .andExpect(jsonPath("$.sequenceNo").value(1))
                .andExpect(jsonPath("$.outcome").isEmpty());

        // 2. Try recording WORSENED without explanation -> should fail with 422
        String invalidOutcomeJson = """
                {
                  "outcome": "WORSENED",
                  "observation": "Checkout latency doubled"
                }
                """;

        mockMvc.perform(patch("/api/incidents/INC-019/actions/1/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidOutcomeJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("explanation"));

        // 3. Try resolving while action outcome is not recorded -> should fail with 409
        String resolveJson = """
                {
                  "rootCause": "Slow query",
                  "lesson": "Do not restart cache"
                }
                """;

        mockMvc.perform(post("/api/incidents/INC-019/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));

        // 4. Record valid WORSENED outcome with explanation -> should succeed
        String validOutcomeJson = """
                {
                  "outcome": "WORSENED",
                  "observation": "Checkout latency doubled within 90 seconds",
                  "explanation": "The restart emptied the cache, so every request hit the database directly"
                }
                """;

        mockMvc.perform(patch("/api/incidents/INC-019/actions/1/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOutcomeJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("WORSENED"))
                .andExpect(jsonPath("$.sequenceNo").value(1));

        // 5. Now resolve should succeed with 200
        mockMvc.perform(post("/api/incidents/INC-019/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentCode").value("INC-019"))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").exists())
                .andExpect(jsonPath("$.memorySync.status").exists());

        // 6. Try resolving again -> should fail with 409
        mockMvc.perform(post("/api/incidents/INC-019/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resolveJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));

        // 7. Verify incident detail endpoint
        mockMvc.perform(get("/api/incidents/INC-019"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("INC-019"))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.actions").isArray())
                .andExpect(jsonPath("$.actions[0].outcome").value("WORSENED"));
    }
}
