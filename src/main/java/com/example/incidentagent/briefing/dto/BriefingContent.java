package com.example.incidentagent.briefing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BriefingContent(
        String source,
        String model,
        List<PrecedentAssessment> precedentAssessment,
        boolean noRelevantPrecedent,
        String summary,
        List<LikelyCause> likelyCauses,
        List<RecommendedAction> recommendedActions,
        List<WarningItem> warnings,
        List<String> gaps
) {
    public record PrecedentAssessment(
            String incidentCode,
            boolean relevant,
            String reason
    ) {}

    public record LikelyCause(
            String text,
            List<String> citedIncidents
    ) {}

    public record RecommendedAction(
            String text,
            List<String> citedIncidents
    ) {}

    public record WarningItem(
            String action,
            CitedAction citedAction,
            String reason,
            WarningEvidence evidence
    ) {
        public WarningItem withEvidence(WarningEvidence ev) {
            return new WarningItem(action, citedAction, reason, ev);
        }
    }

    public record CitedAction(
            String incidentCode,
            int actionSequence
    ) {}

    public record WarningEvidence(
            String source,
            String outcome,
            String recordedAt
    ) {}
}

