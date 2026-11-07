package com.example.incidentagent.integration.hindsight;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.alert.Alert;
import com.example.incidentagent.common.exception.MemoryUnavailableException;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.memory.IncidentMemory;
import com.example.incidentagent.memory.MemoryDocumentFormatter;
import com.example.incidentagent.memory.RecallResult;
import com.example.incidentagent.memory.RecalledMemory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class HindsightMemoryClient implements IncidentMemory {

    private static final Logger log = LoggerFactory.getLogger(HindsightMemoryClient.class);
    private static final Pattern INCIDENT_CODE_PATTERN = Pattern.compile("INC-\\d{3}");

    private final RestClient client;
    private final String apiKey;
    private final String bankId;
    private final MemoryDocumentFormatter formatter;
    private final ObjectMapper objectMapper;

    public HindsightMemoryClient(RestClient.Builder builder,
            @Value("${app.hindsight.api-key:}") String apiKey,
            @Value("${app.hindsight.base-url:https://api.hindsight.vectorize.io}") String baseUrl,
            @Value("${app.hindsight.bank-id:incident-iq-demo}") String bankId,
            MemoryDocumentFormatter formatter,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.bankId = bankId;
        this.formatter = formatter;
        this.objectMapper = objectMapper;
        this.client = builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void retainIncident(Incident incident, List<ActionAttempt> actions) {
        requireKey();
        String narrative = formatter.format(incident, actions);
        String docId = incident.getCode() != null ? incident.getCode() : "INC-000";

        Map<String, Object> payload = Map.of(
                "items", List.of(
                        Map.of(
                                "content", narrative,
                                "document_id", docId,
                                "context", "incident-resolution")));

        try {
            client.post()
                    .uri("/v1/default/banks/{bank}/memories", bankId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully retained incident {} into Hindsight memory", docId);
        } catch (RestClientException ex) {
            log.warn("Hindsight retain failed for {}: {}", docId, ex.getMessage());
            throw new MemoryUnavailableException("Hindsight retain failed: " + ex.getMessage());
        }
    }

    @Override
    public RecallResult recallSimilar(Alert alert, String query) {
        requireKey();
        try {
            String rawJson = client.post()
                    .uri("/v1/default/banks/{bank}/memories/recall", bankId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(Map.of("query", query))
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                return new RecallResult("hindsight", query, Collections.emptyList(), "{}");
            }

            List<RecalledMemory> items = parseRecallItems(rawJson);
            return new RecallResult("hindsight", query, items, rawJson);
        } catch (RestClientException ex) {
            log.warn("Hindsight recall failed: {}", ex.getMessage());
            throw new MemoryUnavailableException("Hindsight recall failed: " + ex.getMessage());
        }
    }

    private List<RecalledMemory> parseRecallItems(String rawJson) {
        List<RecalledMemory> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode itemsNode = null;
            if (root.isArray()) {
                itemsNode = root;
            } else if (root.has("items") && root.get("items").isArray()) {
                itemsNode = root.get("items");
            } else if (root.has("results") && root.get("results").isArray()) {
                itemsNode = root.get("results");
            } else if (root.has("memories") && root.get("memories").isArray()) {
                itemsNode = root.get("memories");
            }

            if (itemsNode != null) {
                int rank = 1;
                for (JsonNode item : itemsNode) {
                    String text = item.has("text") ? item.get("text").asText()
                            : (item.has("content") ? item.get("content").asText() : item.toString());
                    String docId = item.has("document_id") ? item.get("document_id").asText() : null;
                    if (docId == null || docId.isBlank()) {
                        Matcher m = INCIDENT_CODE_PATTERN.matcher(text);
                        if (m.find()) {
                            docId = m.group();
                        }
                    }
                    Double score = item.has("score") && item.get("score").isNumber() ? item.get("score").asDouble()
                            : null;
                    list.add(new RecalledMemory(rank++, docId, text, score));
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse recall JSON response: {}", e.getMessage());
        }
        return list;
    }

    // --- Backward compatibility methods ---

    public String recall(String query) {
        requireKey();
        try {
            Object response = client.post().uri("/v1/default/banks/{bank}/memories/recall", bankId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(Map.of("query", query)).retrieve().body(Object.class);
            return response == null ? "No previous incident memories returned." : response.toString();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Hindsight recall failed: " + ex.getMessage(), ex);
        }
    }

    public void retain(String content) {
        requireKey();
        try {
            client.post().uri("/v1/default/banks/{bank}/memories", bankId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(Map.of("items", List.of(Map.of("content", content))))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Hindsight retain failed: " + ex.getMessage(), ex);
        }
    }

    private void requireKey() {
        if (!StringUtils.hasText(apiKey))
            throw new MemoryUnavailableException("HINDSIGHT_API_KEY is not configured");
    }
}
