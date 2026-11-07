package com.example.incidentagent.briefing.dto;

import com.example.incidentagent.briefing.BriefingStatus;
import com.example.incidentagent.memory.RecalledMemory;
import java.util.List;

public record BriefingResponse(
        String incidentCode,
        Long briefingId,
        BriefingStatus status,
        RetrievedBlock retrieved,
        BriefingContent briefing,
        ValidationResult validation
) {
    public record RetrievedBlock(
            String source,
            String query,
            List<RecalledMemory> items
    ) {}
}
