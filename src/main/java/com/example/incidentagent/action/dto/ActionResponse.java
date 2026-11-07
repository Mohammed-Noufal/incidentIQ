package com.example.incidentagent.action.dto;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.action.Outcome;
import java.time.Instant;

public record ActionResponse(
        String incidentCode,
        int sequenceNo,
        String actionDescription,
        String targetService,
        Outcome outcome,
        String observation,
        String explanation,
        Instant performedAt,
        Instant outcomeRecordedAt
) {
    public static ActionResponse from(ActionAttempt a) {
        String code = a.getIncident() != null ? a.getIncident().getCode() : null;
        String target = a.getTargetService() != null ? a.getTargetService().getName() : null;
        return new ActionResponse(
                code,
                a.getSequenceNo(),
                a.getActionDescription(),
                target,
                a.getOutcome(),
                a.getObservation(),
                a.getExplanation(),
                a.getPerformedAt(),
                a.getOutcomeRecordedAt()
        );
    }
}

