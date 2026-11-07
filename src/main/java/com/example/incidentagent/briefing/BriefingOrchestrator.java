package com.example.incidentagent.briefing;

import com.example.incidentagent.alert.Alert;
import com.example.incidentagent.alert.AlertRepository;
import com.example.incidentagent.briefing.BriefingValidator.ValidatedBriefing;
import com.example.incidentagent.briefing.dto.BriefingContent;
import com.example.incidentagent.briefing.dto.BriefingResponse;
import com.example.incidentagent.briefing.dto.BriefingResponse.RetrievedBlock;
import com.example.incidentagent.briefing.dto.ValidationResult;
import com.example.incidentagent.common.exception.NotFoundException;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentRepository;
import com.example.incidentagent.integration.groq.GroqClient;
import com.example.incidentagent.memory.IncidentMemory;
import com.example.incidentagent.memory.RecallResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BriefingOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(BriefingOrchestrator.class);

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final RecallQueryBuilder recallQueryBuilder;
    private final IncidentMemory incidentMemory;
    private final BriefingPromptBuilder promptBuilder;
    private final GroqClient groqClient;
    private final BriefingValidator briefingValidator;
    private final BriefingRecordService recordService;
    private final ObjectMapper objectMapper;

    public BriefingOrchestrator(IncidentRepository incidentRepository,
                                AlertRepository alertRepository,
                                RecallQueryBuilder recallQueryBuilder,
                                IncidentMemory incidentMemory,
                                BriefingPromptBuilder promptBuilder,
                                GroqClient groqClient,
                                BriefingValidator briefingValidator,
                                BriefingRecordService recordService,
                                ObjectMapper objectMapper) {
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.recallQueryBuilder = recallQueryBuilder;
        this.incidentMemory = incidentMemory;
        this.promptBuilder = promptBuilder;
        this.groqClient = groqClient;
        this.briefingValidator = briefingValidator;
        this.recordService = recordService;
        this.objectMapper = objectMapper;
    }

    public BriefingResponse generateBriefing(String incidentCode) {
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new NotFoundException("Incident '" + incidentCode + "' not found"));

        List<Alert> alerts = alertRepository.findByIncidentIdOrderByReceivedAtAsc(incident.getId());
        Alert primaryAlert = !alerts.isEmpty() ? alerts.get(0) : createSyntheticAlertFromIncident(incident);

        // Step 1: Recall from Hindsight
        String recallQuery = recallQueryBuilder.buildQuery(primaryAlert);
        long recallStart = System.currentTimeMillis();
        RecallResult recallResult = incidentMemory.recallSimilar(primaryAlert, recallQuery);
        int latencyRecall = (int) (System.currentTimeMillis() - recallStart);

        // Step 2: Prompt construction
        String systemPrompt = promptBuilder.getSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(primaryAlert, recallResult.items());

        // Step 3: LLM reasoning via Groq
        BriefingStatus status = BriefingStatus.COMPLETE;
        String rawLlmResponse = null;
        BriefingContent parsedContent = null;
        int latencyLlm = 0;

        long llmStart = System.currentTimeMillis();
        try {
            rawLlmResponse = groqClient.generateBriefing(systemPrompt, userPrompt);
            latencyLlm = (int) (System.currentTimeMillis() - llmStart);
            parsedContent = parseBriefingJson(rawLlmResponse);
        } catch (Exception ex) {
            latencyLlm = (int) (System.currentTimeMillis() - llmStart);
            log.warn("Groq reasoning failed, entering DEGRADED mode: {}", ex.getMessage());
            status = BriefingStatus.DEGRADED;
        }

        // Step 4: Deterministic validation
        ValidatedBriefing validated = null;
        if (parsedContent != null) {
            // Set source and model info
            parsedContent = new BriefingContent(
                    "groq",
                    groqClient.getModel(),
                    parsedContent.precedentAssessment(),
                    parsedContent.noRelevantPrecedent(),
                    parsedContent.summary(),
                    parsedContent.likelyCauses(),
                    parsedContent.recommendedActions(),
                    parsedContent.warnings(),
                    parsedContent.gaps()
            );
            validated = briefingValidator.validate(parsedContent, recallResult.items());
        } else {
            validated = new ValidatedBriefing(null, new ValidationResult(true, Collections.emptyList()));
        }

        // Step 5: Save audit record
        BriefingRecord record = recordService.recordBriefing(
                incident,
                status,
                recallQuery,
                recallResult.rawResponse(),
                rawLlmResponse,
                validated.content(),
                validated.validationResult(),
                latencyRecall,
                latencyLlm
        );

        return new BriefingResponse(
                incident.getCode(),
                record.getId(),
                status,
                new RetrievedBlock("hindsight", recallQuery, recallResult.items()),
                validated.content(),
                validated.validationResult()
        );
    }

    public BriefingResponse getLatestBriefing(String incidentCode) {
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new NotFoundException("Incident '" + incidentCode + "' not found"));

        BriefingRecord record = recordService.getLatestForIncident(incident.getId())
                .orElseThrow(() -> new NotFoundException("No briefing found for incident '" + incidentCode + "'"));

        BriefingContent content = null;
        ValidationResult validation = null;
        try {
            if (record.getBriefingJson() != null) {
                content = objectMapper.readValue(record.getBriefingJson(), BriefingContent.class);
            }
            if (record.getValidationJson() != null) {
                validation = objectMapper.readValue(record.getValidationJson(), ValidationResult.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse stored briefing JSON: {}", e.getMessage());
        }

        return new BriefingResponse(
                incident.getCode(),
                record.getId(),
                record.getStatus(),
                new RetrievedBlock("hindsight", record.getRecallQuery(), Collections.emptyList()),
                content,
                validation != null ? validation : new ValidationResult(true, Collections.emptyList())
        );
    }

    private BriefingContent parseBriefingJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, BriefingContent.class);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse briefing JSON from Groq: {}", ex.getMessage());
            return null;
        }
    }

    private Alert createSyntheticAlertFromIncident(Incident inc) {
        Alert a = new Alert();
        a.setIncident(inc);
        a.setService(inc.getService());
        a.setSeverity(inc.getSeverity());
        a.setTitle(inc.getTitle());
        a.setMessage(inc.getSymptoms() != null ? inc.getSymptoms() : inc.getTitle());
        return a;
    }
}

