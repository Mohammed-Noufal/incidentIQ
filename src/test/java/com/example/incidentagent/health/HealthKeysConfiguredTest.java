package com.example.incidentagent.health;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** With keys present the health endpoint says CONFIGURED, and never echoes the key values. */
@SpringBootTest(properties = {
        "app.groq.api-key=fake-groq-secret-value",
        "app.hindsight.api-key=fake-hindsight-secret-value"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthKeysConfiguredTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reportsConfiguredWithoutLeakingKeys() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.groq.status").value("CONFIGURED"))
                .andExpect(jsonPath("$.components.hindsight.status").value("CONFIGURED"))
                .andExpect(content().string(not(containsString("fake-groq-secret-value"))))
                .andExpect(content().string(not(containsString("fake-hindsight-secret-value"))));
    }
}
