package com.example.incidentagent.incident;

import com.example.incidentagent.catalog.MonitoredService;
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
@Table(name = "incident")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. INC-019. Assigned from the row id right after insert (Step 3). Also the Hindsight document_id. */
    @Column(length = 20, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private MonitoredService service;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(columnDefinition = "TEXT")
    private String lesson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentSource source = IncidentSource.LIVE;

    /** Seed metadata for the held-out check. Never sent to Hindsight. */
    @Column(name = "failure_family", length = 100)
    private String failureFamily;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_sync_status", nullable = false, length = 20)
    private MemorySyncStatus memorySyncStatus = MemorySyncStatus.PENDING;

    @Column(name = "memory_synced_at")
    private Instant memorySyncedAt;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    void onCreate() {
        if (openedAt == null) {
            openedAt = Instant.now();
        }
    }

    public Long getId() { return id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public MonitoredService getService() { return service; }
    public void setService(MonitoredService service) { this.service = service; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getLesson() { return lesson; }
    public void setLesson(String lesson) { this.lesson = lesson; }

    public IncidentSource getSource() { return source; }
    public void setSource(IncidentSource source) { this.source = source; }

    public String getFailureFamily() { return failureFamily; }
    public void setFailureFamily(String failureFamily) { this.failureFamily = failureFamily; }

    public MemorySyncStatus getMemorySyncStatus() { return memorySyncStatus; }
    public void setMemorySyncStatus(MemorySyncStatus memorySyncStatus) { this.memorySyncStatus = memorySyncStatus; }

    public Instant getMemorySyncedAt() { return memorySyncedAt; }
    public void setMemorySyncedAt(Instant memorySyncedAt) { this.memorySyncedAt = memorySyncedAt; }

    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
