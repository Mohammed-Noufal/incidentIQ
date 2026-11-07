package com.example.incidentagent.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentRepository;
import com.example.incidentagent.incident.IncidentStatus;
import com.example.incidentagent.integration.hindsight.HindsightMemoryClient;
import java.util.List;
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
class AdminSeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentRepository incidentRepository;

    @MockitoBean
    private HindsightMemoryClient hindsightMemoryClient;

    @Test
    void seedsDatabaseAndIsIdempotent() throws Exception {
        // First seed run
        mockMvc.perform(post("/api/admin/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.incidentsCreated").value(15));

        List<Incident> all = incidentRepository.findAll();
        assertThat(all).hasSize(15);
        for (Incident inc : all) {
            assertThat(inc.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
            assertThat(inc.getFailureFamily()).isNotEqualToIgnoringCase("connection-pool-exhaustion");
        }

        // Second seed run should be idempotent (0 newly created)
        mockMvc.perform(post("/api/admin/seed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentsCreated").value(0));

        assertThat(incidentRepository.findAll()).hasSize(15);
    }
}
