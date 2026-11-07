package com.example.incidentagent.briefing;

import com.example.incidentagent.incident.Incident;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Audit trail for one briefing: what was sent to Hindsight, what came back, what the LLM said,
 * and what the validator kept or removed.
 * The JSON columns are held as raw JSON text (String); the briefing step fills them in Step 3.
 */
@Entity
@Table(name = "briefing_record")
public class BriefingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BriefingStatus status;

    /** Exactly what was sent to Hindsight. */
    @Column(name = "recall_query", nullable = false, columnDefinition = "TEXT")
    private String recallQuery;

    /** Raw recall response, never edited. */
    @Column(name = "hindsight_raw", columnDefinition = "json")
    private String hindsightRaw;

    /** Raw LLM (Groq) response. */
    @Column(name = "llm_raw", columnDefinition = "json")
    private String llmRaw;

    /** Validated briefing as returned to the client. */
    @Column(name = "briefing_json", columnDefinition = "json")
    private String briefingJson;

    /** Which warnings passed or were removed, and why. */
    @Column(name = "validation_json", columnDefinition = "json")
    private String validationJson;

    @Column(name = "latency_ms_recall")
    private Integer latencyMsRecall;

    @Column(name = "latency_ms_llm")
    private Integer latencyMsLlm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }

    public BriefingStatus getStatus() { return status; }
    public void setStatus(BriefingStatus status) { this.status = status; }

    public String getRecallQuery() { return recallQuery; }
    public void setRecallQuery(String recallQuery) { this.recallQuery = recallQuery; }

    public String getHindsightRaw() { return hindsightRaw; }
    public void setHindsightRaw(String hindsightRaw) { this.hindsightRaw = hindsightRaw; }

    public String getLlmRaw() { return llmRaw; }
    public void setLlmRaw(String llmRaw) { this.llmRaw = llmRaw; }

    public String getBriefingJson() { return briefingJson; }
    public void setBriefingJson(String briefingJson) { this.briefingJson = briefingJson; }

    public String getValidationJson() { return validationJson; }
    public void setValidationJson(String validationJson) { this.validationJson = validationJson; }

    public Integer getLatencyMsRecall() { return latencyMsRecall; }
    public void setLatencyMsRecall(Integer latencyMsRecall) { this.latencyMsRecall = latencyMsRecall; }

    public Integer getLatencyMsLlm() { return latencyMsLlm; }
    public void setLatencyMsLlm(Integer latencyMsLlm) { this.latencyMsLlm = latencyMsLlm; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
