package com.example.incidentagent.memory;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.alert.Alert;
import com.example.incidentagent.incident.Incident;
import java.util.List;

public interface IncidentMemory {

    /**
     * Retains a resolved incident and its attempted actions into Hindsight memory.
     *
     * @param incident the resolved incident
     * @param actions  the ordered list of action attempts
     */
    void retainIncident(Incident incident, List<ActionAttempt> actions);

    /**
     * Recalls similar past incidents from Hindsight memory.
     *
     * @param alert the current incident alert
     * @param query the deterministic recall query
     * @return RecallResult containing parsed memories and raw payload
     */
    RecallResult recallSimilar(Alert alert, String query);
}

