package com.example.incidentagent.briefing;

/** COMPLETE = briefing generated; DEGRADED = memory retrieved but the LLM failed; FAILED = nothing usable. */
public enum BriefingStatus {
    COMPLETE, DEGRADED, FAILED
}
