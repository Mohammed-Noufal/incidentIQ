package com.example.incidentagent.briefing;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.incidentagent.catalog.MonitoredService;
import com.example.incidentagent.catalog.MonitoredServiceRepository;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentRepository;
import com.example.incidentagent.incident.Severity;
import com.example.incidentagent.integration.groq.GroqClient;
import com.example.incidentagent.integration.hindsight.HindsightMemoryClient;
import com.example.incidentagent.memory.RecallResult;
import com.example.incidentagent.memory.RecalledMemory;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Transactional
class BriefingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private MonitoredServiceRepository serviceRepository;

    @MockitoBean
    private HindsightMemoryClient hindsightMemoryClient;

    @MockitoBean
    private GroqClient groqClient;

    @BeforeEach
    void setUp() {
        MonitoredService s = serviceRepository.save(new MonitoredService("payments-worker", "Payments"));
        Incident inc = new Incident();
        inc.setCode("INC-019");
        inc.setService(s);
        inc.setTitle("Checkout latency above 8s");
        inc.setSymptoms("Connection timed out");
        inc.setSeverity(Severity.HIGH);
        incidentRepository.save(inc);
    }

    @Test
    void generatesCompleteBriefingWhenPrecedentFound() throws Exception {
        RecalledMemory memory = new RecalledMemory(1, "INC-007", "INC-007 | orders-api | disk usage 98%", 0.85);
        when(hindsightMemoryClient.recallSimilar(any(), anyString()))
                .thenReturn(new RecallResult("hindsight", "query", List.of(memory), "{}"));

        when(groqClient.getModel()).thenReturn("openai/gpt-oss-20b");
        String groqResponse = """
                {
                  "precedentAssessment": [
                    { "incidentCode": "INC-007", "relevant": false, "reason": "Different failure mode" }
                  ],
                  "noRelevantPrecedent": true,
                  "summary": "Connection starvation issue",
                  "likelyCauses": [
                    { "text": "Connection pool exhaustion", "citedIncidents": [] }
                  ],
                  "recommendedActions": [
                    { "text": "Inspect active queries", "citedIncidents": [] }
                  ],
                  "warnings": [],
                  "gaps": ["No exact match"]
                }
                """;
        when(groqClient.generateBriefing(anyString(), anyString())).thenReturn(groqResponse);

        mockMvc.perform(post("/api/incidents/INC-019/briefing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentCode").value("INC-019"))
                .andExpect(jsonPath("$.status").value("COMPLETE"))
                .andExpect(jsonPath("$.retrieved.source").value("hindsight"))
                .andExpect(jsonPath("$.retrieved.items[0].incidentCode").value("INC-007"))
                .andExpect(jsonPath("$.briefing.source").value("groq"))
                .andExpect(jsonPath("$.briefing.noRelevantPrecedent").value(true))
                .andExpect(jsonPath("$.briefing.summary").value("Connection starvation issue"))
                .andExpect(jsonPath("$.validation.passed").value(true));

        // And GET latest briefing should return the recorded briefing
        mockMvc.perform(get("/api/incidents/INC-019/briefing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentCode").value("INC-019"))
                .andExpect(jsonPath("$.status").value("COMPLETE"));
    }

    @Test
    void degradesGracefullyWhenGroqFails() throws Exception {
        when(hindsightMemoryClient.recallSimilar(any(), anyString()))
                .thenReturn(new RecallResult("hindsight", "query", List.of(), "{}"));

        when(groqClient.generateBriefing(anyString(), anyString()))
                .thenThrow(new IllegalStateException("Groq rate limit reached"));

        mockMvc.perform(post("/api/incidents/INC-019/briefing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.retrieved.source").value("hindsight"))
                .andExpect(jsonPath("$.briefing").value(nullValue()));
    }
}
