package com.example.incidentagent.seed;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Enforces constraint C1 and Section 9.5 rules:
 * Confirms that no seeded incident belongs to the held-out failure family (connection-pool-exhaustion).
 */
@Component
public class SeedGuard {

    public static final String HELD_OUT_FAILURE_FAMILY = "connection-pool-exhaustion";

    public void validate(List<SeedIncidentDto> incidents) {
        if (incidents == null) return;
        for (SeedIncidentDto inc : incidents) {
            if (inc.failureFamily() != null && HELD_OUT_FAILURE_FAMILY.equalsIgnoreCase(inc.failureFamily().trim())) {
                throw new IllegalStateException("Seed guard violation: seed incident " + inc.code()
                        + " carries held-out failure family '" + inc.failureFamily() + "'");
            }
        }
    }
}

