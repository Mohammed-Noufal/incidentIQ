package com.example.incidentagent.briefing;

import com.example.incidentagent.briefing.dto.BriefingContent;
import com.example.incidentagent.briefing.dto.ValidationResult;
import com.example.incidentagent.incident.Incident;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BriefingRecordService {

    private static final Logger log = LoggerFactory.getLogger(BriefingRecordService.class);

    private final BriefingRecordRepository repository;
    private final ObjectMapper objectMapper;

    public BriefingRecordService(BriefingRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BriefingRecord recordBriefing(Incident incident,
                                         BriefingStatus status,
                                         String recallQuery,
                                         String hindsightRaw,
                                         String llmRaw,
                                         BriefingContent briefing,
                                         ValidationResult validation,
                                         Integer latencyMsRecall,
                                         Integer latencyMsLlm) {
        BriefingRecord record = new BriefingRecord();
        record.setIncident(incident);
        record.setStatus(status);
        record.setRecallQuery(recallQuery != null ? recallQuery : "");
        record.setHindsightRaw(hindsightRaw);
        record.setLlmRaw(llmRaw);
        record.setLatencyMsRecall(latencyMsRecall);
        record.setLatencyMsLlm(latencyMsLlm);
        record.setCreatedAt(Instant.now());

        try {
            if (briefing != null) {
                record.setBriefingJson(objectMapper.writeValueAsString(briefing));
            }
            if (validation != null) {
                record.setValidationJson(objectMapper.writeValueAsString(validation));
            }
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize briefing or validation to JSON: {}", ex.getMessage());
        }

        return repository.save(record);
    }

    @Transactional(readOnly = true)
    public Optional<BriefingRecord> getLatestForIncident(Long incidentId) {
        return repository.findFirstByIncidentIdOrderByCreatedAtDesc(incidentId);
    }
}

