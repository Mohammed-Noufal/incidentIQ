package com.example.incidentagent.action.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateActionRequest(
        @NotBlank(message = "actionDescription is required")
        String actionDescription,

        String targetService
) {}

