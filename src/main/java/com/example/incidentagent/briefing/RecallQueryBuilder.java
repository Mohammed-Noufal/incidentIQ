package com.example.incidentagent.briefing;

import com.example.incidentagent.alert.Alert;
import org.springframework.stereotype.Component;

/**
 * Deterministically constructs a recall query string from an alert.
 * Describes the situation rather than prescribing solutions so semantic recall matches
 * similar underlying failure patterns across microservices.
 */
@Component
public class RecallQueryBuilder {

    public String buildQuery(Alert alert) {
        StringBuilder sb = new StringBuilder();
        if (alert.getService() != null) {
            sb.append(alert.getService().getName()).append(": ");
        }
        if (alert.getTitle() != null && !alert.getTitle().isBlank()) {
            sb.append(alert.getTitle().trim()).append(". ");
        }
        if (alert.getMessage() != null && !alert.getMessage().isBlank()) {
            sb.append(alert.getMessage().trim()).append(" ");
        }
        if (alert.getLogExcerpt() != null && !alert.getLogExcerpt().isBlank()) {
            String excerpt = alert.getLogExcerpt().trim();
            if (excerpt.length() > 300) {
                excerpt = excerpt.substring(0, 300);
            }
            sb.append("Log excerpt: ").append(excerpt);
        }
        return sb.toString().trim();
    }
}

