package com.example.incidentagent.demo;

import jakarta.validation.constraints.NotBlank;

public record InvestigationRequest(
        @NotBlank String serviceName,
        @NotBlank String title,
        @NotBlank String symptoms,
        String severity) { }
