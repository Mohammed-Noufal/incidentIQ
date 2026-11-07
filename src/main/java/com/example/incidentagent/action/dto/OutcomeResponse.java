package com.example.incidentagent.action.dto;

import com.example.incidentagent.action.Outcome;
import java.time.Instant;

public record OutcomeResponse(
        String incidentCode,
        int sequenceNo,
        Outcome outcome,
        Instant outcomeRecordedAt
) {}

