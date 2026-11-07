package com.example.incidentagent.memory;

import java.util.List;

public record RecallResult(
        String source,
        String query,
        List<RecalledMemory> items,
        String rawResponse
) {}

