package com.example.incidentagent.incident.dto;

import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentSource;
import com.example.incidentagent.incident.IncidentStatus;
import com.example.incidentagent.incident.MemorySyncStatus;
import com.example.incidentagent.incident.Severity;
import java.time.Instant;

public record IncidentResponse(
        String code,
        String title,
        String service,
        Severity severity,
        IncidentStatus status,
        String symptoms,
        String rootCause,
        String lesson,
        IncidentSource source,
        MemorySyncStatus memorySyncStatus,
        Instant openedAt,
        Instant resolvedAt
) {
    public static IncidentResponse from(Incident inc) {
        return new IncidentResponse(
                inc.getCode(),
                inc.getTitle(),
                inc.getService() != null ? inc.getService().getName() : null,
                inc.getSeverity(),
                inc.getStatus(),
                inc.getSymptoms(),
                inc.getRootCause(),
                inc.getLesson(),
                inc.getSource(),
                inc.getMemorySyncStatus(),
                inc.getOpenedAt(),
                inc.getResolvedAt()
        );
    }
}

