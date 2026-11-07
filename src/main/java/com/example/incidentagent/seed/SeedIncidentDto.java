package com.example.incidentagent.seed;

import com.example.incidentagent.action.Outcome;
import com.example.incidentagent.incident.Severity;
import java.util.List;

public record SeedIncidentDto(
        String code,
        String serviceName,
        String title,
        String symptoms,
        Severity severity,
        String failureFamily,
        String rootCause,
        String lesson,
        List<SeedActionDto> actions
) {
    public record SeedActionDto(
            int sequenceNo,
            String actionDescription,
            String targetService,
            Outcome outcome,
            String observation,
            String explanation
    ) {}
}

