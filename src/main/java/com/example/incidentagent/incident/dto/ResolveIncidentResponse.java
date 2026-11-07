package com.example.incidentagent.incident.dto;

import com.example.incidentagent.incident.IncidentStatus;
import com.example.incidentagent.incident.MemorySyncStatus;
import java.time.Instant;

public record ResolveIncidentResponse(
        String incidentCode,
        IncidentStatus status,
        Instant resolvedAt,
        MemorySyncInfo memorySync
) {
    public record MemorySyncInfo(
            MemorySyncStatus status,
            String documentId
    ) {}
}

