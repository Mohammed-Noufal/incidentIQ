package com.example.incidentagent.integration.groq;

import com.example.incidentagent.common.exception.LlmUnavailableException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final RestClient client;
    private final String apiKey;
    private final String model;

    public GroqClient(RestClient.Builder builder,
                      @Value("${app.groq.api-key:}") String apiKey,
                      @Value("${app.groq.model:openai/gpt-oss-20b}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = builder.baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

    public String getModel() {
        return model;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * Generates a structured briefing from Groq using JSON response format.
     * Retries once on transient failure.
     */
    public String generateBriefing(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new LlmUnavailableException("GROQ_API_KEY is not configured");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        int maxAttempts = 2;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Map<?, ?> response = client.post().uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .body(body).retrieve().body(Map.class);

                if (response == null || !(response.get("choices") instanceof List<?> choices) || choices.isEmpty()) {
                    throw new IllegalStateException("Groq returned an empty response");
                }
                Object message = ((Map<?, ?>) choices.get(0)).get("message");
                Object content = message instanceof Map<?, ?> map ? map.get("content") : null;
                if (!(content instanceof String text) || !StringUtils.hasText(text)) {
                    throw new IllegalStateException("Groq response did not contain message content");
                }
                return text;
            } catch (RestClientException | IllegalStateException ex) {
                lastException = ex;
                log.warn("Groq briefing generation attempt {} failed: {}", attempt, ex.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new LlmUnavailableException("Groq request failed: " + (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    /**
     * Existing demo analyze method preserved for backward compatibility.
     */
    public String analyze(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(apiKey)) throw new IllegalStateException("GROQ_API_KEY is not configured");
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        try {
            Map<?, ?> response = client.post().uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(body).retrieve().body(Map.class);
            if (response == null || !(response.get("choices") instanceof List<?> choices) || choices.isEmpty())
                throw new IllegalStateException("Groq returned an empty response");
            Object message = ((Map<?, ?>) choices.get(0)).get("message");
            Object content = message instanceof Map<?, ?> map ? map.get("content") : null;
            if (!(content instanceof String text) || !StringUtils.hasText(text))
                throw new IllegalStateException("Groq response did not contain message content");
            return text;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Groq request failed: " + ex.getMessage(), ex);
        }
    }
}
