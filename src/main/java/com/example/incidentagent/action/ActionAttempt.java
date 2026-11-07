package com.example.incidentagent.action;

import com.example.incidentagent.catalog.MonitoredService;
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

@Entity
@Table(name = "action_attempt")
public class ActionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    /** Unique per incident; lets a warning cite "action 1 of INC-019" without exposing database ids. */
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "action_description", nullable = false, columnDefinition = "TEXT")
    private String actionDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_service_id")
    private MonitoredService targetService;

    /** Null until the outcome is recorded. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Outcome outcome;

    @Column(columnDefinition = "TEXT")
    private String observation;

    /** Why it happened (mechanism). Required when the outcome is WORSENED (enforced in a later step). */
    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "outcome_recorded_at")
    private Instant outcomeRecordedAt;

    @PrePersist
    void onCreate() {
        if (performedAt == null) {
            performedAt = Instant.now();
        }
    }

    public Long getId() { return id; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public String getActionDescription() { return actionDescription; }
    public void setActionDescription(String actionDescription) { this.actionDescription = actionDescription; }

    public MonitoredService getTargetService() { return targetService; }
    public void setTargetService(MonitoredService targetService) { this.targetService = targetService; }

    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }

    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public Instant getPerformedAt() { return performedAt; }
    public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }

    public Instant getOutcomeRecordedAt() { return outcomeRecordedAt; }
    public void setOutcomeRecordedAt(Instant outcomeRecordedAt) { this.outcomeRecordedAt = outcomeRecordedAt; }
}
