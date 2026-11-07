package com.example.incidentagent.memory;

public record RecalledMemory(
        int rank,
        String incidentCode,
        String text,
        Double score
) {}

