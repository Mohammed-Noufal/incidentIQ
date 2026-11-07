package com.example.incidentagent.action.dto;

import com.example.incidentagent.action.Outcome;
import jakarta.validation.constraints.NotNull;

public record RecordOutcomeRequest(
        @NotNull(message = "outcome is required")
        Outcome outcome,

        String observation,

        String explanation
) {}

