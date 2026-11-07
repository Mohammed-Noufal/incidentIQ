package com.example.incidentagent.memory;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.incident.Incident;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Formats an incident and its actions into the deterministic narrative text retained by Hindsight.
 * Follows Section 9.2 rules: repeating incident code in outcome and mechanism lines to ensure
 * facts extracted by Hindsight remain attributed to their specific incident.
 */
@Component
public class MemoryDocumentFormatter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd").withZone(ZoneOffset.UTC);

    public String format(Incident incident, List<ActionAttempt> actions) {
        StringBuilder sb = new StringBuilder();

        String code = incident.getCode() != null ? incident.getCode() : "INC-000";
        String serviceName = incident.getService() != null ? incident.getService().getName() : "unknown-service";
        String dateStr = incident.getOpenedAt() != null ? DATE_FORMAT.format(incident.getOpenedAt()) : "2026-11-05";

        sb.append(code).append(" | ").append(serviceName).append(" | ").append(dateStr).append("\n");

        if (incident.getSymptoms() != null && !incident.getSymptoms().isBlank()) {
            sb.append("Symptoms: ").append(incident.getSymptoms().trim()).append("\n");
        } else if (incident.getTitle() != null) {
            sb.append("Symptoms: ").append(incident.getTitle().trim()).append("\n");
        }

        if (incident.getRootCause() != null && !incident.getRootCause().isBlank()) {
            sb.append("Root cause: ").append(incident.getRootCause().trim()).append("\n");
        }

        if (actions != null) {
            for (ActionAttempt action : actions) {
                int seq = action.getSequenceNo();
                sb.append("Action ").append(seq).append(" in ").append(code).append(": ")
                        .append(action.getActionDescription() != null ? action.getActionDescription().trim() : "")
                        .append("\n");

                if (action.getOutcome() != null) {
                    sb.append("Outcome of action ").append(seq).append(" in ").append(code).append(": ")
                            .append(action.getOutcome().name()).append(".\n");
                }

                if (action.getExplanation() != null && !action.getExplanation().isBlank()) {
                    sb.append("Mechanism for action ").append(seq).append(" in ").append(code).append(": ")
                            .append(action.getExplanation().trim()).append("\n");
                }

                if (action.getObservation() != null && !action.getObservation().isBlank()) {
                    sb.append("Observation for action ").append(seq).append(" in ").append(code).append(": ")
                            .append(action.getObservation().trim()).append("\n");
                }
            }
        }

        if (incident.getLesson() != null && !incident.getLesson().isBlank()) {
            sb.append("Lesson from ").append(code).append(": ").append(incident.getLesson().trim()).append("\n");
        }

        return sb.toString();
    }
}

