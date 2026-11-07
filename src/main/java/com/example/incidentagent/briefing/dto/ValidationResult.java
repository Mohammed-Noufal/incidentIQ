package com.example.incidentagent.briefing.dto;

import java.util.List;

public record ValidationResult(
        boolean passed,
        List<RemovedWarning> removedWarnings
) {
    public record RemovedWarning(
            String action,
            String citedIncident,
            Integer actionSequence,
            String reason
    ) {}
}

