package com.example.incidentagent.incident.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveIncidentRequest(
        @NotBlank(message = "rootCause is required")
        String rootCause,

        @NotBlank(message = "lesson is required")
        String lesson
) {}

