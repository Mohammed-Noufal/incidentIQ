package com.example.incidentagent.alert.dto;

import com.example.incidentagent.incident.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAlertRequest(
        @NotBlank(message = "serviceName is required")
        String serviceName,

        @NotNull(message = "severity is required")
        Severity severity,

        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "message is required")
        String message,

        @Size(max = 10000, message = "logExcerpt must not exceed 10000 characters")
        String logExcerpt
) {}

