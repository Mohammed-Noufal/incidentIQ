package com.example.incidentagent.demo;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.incidentagent.integration.groq.GroqClient;
import com.example.incidentagent.integration.hindsight.HindsightMemoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class InvestigationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HindsightMemoryClient hindsight;

    @MockitoBean
    private GroqClient groq;

    @Test
    void demoInvestigateEndpointWorks() throws Exception {
        when(hindsight.recall(anyString())).thenReturn("Prior memory text");
        when(groq.analyze(anyString(), anyString())).thenReturn("AI analysis markdown");

        String json = """
                {
                  "serviceName": "Payment API",
                  "title": "Elevated payment latency",
                  "severity": "HIGH",
                  "symptoms": "p95 latency increased"
                }
                """;

        mockMvc.perform(post("/api/demo/investigate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").value("AI analysis markdown"))
                .andExpect(jsonPath("$.memoryContext").value("Prior memory text"));
    }
}
