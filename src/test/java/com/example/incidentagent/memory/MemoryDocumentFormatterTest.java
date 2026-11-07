package com.example.incidentagent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.action.Outcome;
import com.example.incidentagent.catalog.MonitoredService;
import com.example.incidentagent.incident.Incident;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MemoryDocumentFormatterTest {

    private final MemoryDocumentFormatter formatter = new MemoryDocumentFormatter();

    @Test
    void formatsIncidentNarrativeAccordingToSection9Rules() {
        MonitoredService service = new MonitoredService("payments-worker", "Payments");
        Incident incident = new Incident();
        incident.setCode("INC-019");
        incident.setService(service);
        incident.setTitle("Checkout latency above 8s");
        incident.setSymptoms("checkout latency above 8s; connection not available");
        incident.setRootCause("A slow report query held database connections, exhausting the pool.");
        incident.setLesson("Do not restart cache-service while the database is saturated.");
        incident.setOpenedAt(Instant.parse("2026-11-05T02:00:00Z"));

        ActionAttempt action1 = new ActionAttempt();
        action1.setSequenceNo(1);
        action1.setActionDescription("Restarted cache-service");
        action1.setOutcome(Outcome.WORSENED);
        action1.setExplanation("The restart emptied the cache, so every request hit the database directly");

        ActionAttempt action2 = new ActionAttempt();
        action2.setSequenceNo(2);
        action2.setActionDescription("Terminated slow query");
        action2.setOutcome(Outcome.RESOLVED);

        String narrative = formatter.format(incident, List.of(action1, action2));

        assertThat(narrative).contains("INC-019 | payments-worker | 2026-11-05");
        assertThat(narrative).contains("Symptoms: checkout latency above 8s; connection not available");
        assertThat(narrative).contains("Root cause: A slow report query held database connections, exhausting the pool.");
        assertThat(narrative).contains("Action 1 in INC-019: Restarted cache-service");
        assertThat(narrative).contains("Outcome of action 1 in INC-019: WORSENED.");
        assertThat(narrative).contains("Mechanism for action 1 in INC-019: The restart emptied the cache, so every request hit the database directly");
        assertThat(narrative).contains("Action 2 in INC-019: Terminated slow query");
        assertThat(narrative).contains("Outcome of action 2 in INC-019: RESOLVED.");
        assertThat(narrative).contains("Lesson from INC-019: Do not restart cache-service while the database is saturated.");
    }
}

