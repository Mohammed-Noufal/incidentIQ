package com.example.incidentagent.incident.dto;

import com.example.incidentagent.action.dto.ActionResponse;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentSource;
import com.example.incidentagent.incident.IncidentStatus;
import com.example.incidentagent.incident.MemorySyncStatus;
import com.example.incidentagent.incident.Severity;
import java.time.Instant;
import java.util.List;

public record IncidentDetailResponse(
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
        Instant resolvedAt,
        List<ActionResponse> actions,
        List<AlertSummary> alerts
) {
    public record AlertSummary(
            Long id,
            String severity,
            String title,
            String message,
            String logExcerpt,
            Instant receivedAt
    ) {}

    public static IncidentDetailResponse from(Incident inc, List<ActionResponse> actions, List<AlertSummary> alerts) {
        return new IncidentDetailResponse(
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
                inc.getResolvedAt(),
                actions,
                alerts
        );
    }
}

