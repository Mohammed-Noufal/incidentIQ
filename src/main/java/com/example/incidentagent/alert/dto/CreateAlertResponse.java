package com.example.incidentagent.alert.dto;

import com.example.incidentagent.incident.IncidentStatus;
import com.example.incidentagent.incident.Severity;
import java.time.Instant;

public record CreateAlertResponse(
        Long alertId,
        IncidentSummary incident
) {
    public record IncidentSummary(
            String code,
            IncidentStatus status,
            String service,
            Severity severity,
            Instant openedAt
    ) {}
}

