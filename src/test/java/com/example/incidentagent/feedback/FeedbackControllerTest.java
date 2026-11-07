package com.example.incidentagent.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Test
    void submitValidFeedbackSavesAndReturns201() throws Exception {
        String json = """
                {
                  "name": "Alex Engineer",
                  "email": "alex@example.com",
                  "rating": 5,
                  "message": "The automated verification against MySQL prevented a serious outage."
                }
                """;

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Alex Engineer"))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        assertThat(feedbackRepository.findAll())
                .anyMatch(f -> f.getEmail().equals("alex@example.com") && f.getRating() == 5);
    }

    @Test
    void invalidFeedbackFailsValidation() throws Exception {
        String invalidJson = """
                {
                  "name": "",
                  "email": "not-an-email",
                  "rating": 7,
                  "message": ""
                }
                """;

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void honeypotSpamIsSilentlyDiscarded() throws Exception {
        String spamJson = """
                {
                  "name": "Spam Bot",
                  "email": "bot@spam.com",
                  "rating": 1,
                  "message": "Buy cheap stuff",
                  "website": "http://spamsite.xyz"
                }
                """;

        mockMvc.perform(post("/api/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spamJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(-1));

        assertThat(feedbackRepository.findAll())
                .noneMatch(f -> f.getEmail().equals("bot@spam.com"));
    }
}

