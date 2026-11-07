package com.example.incidentagent.feedback.dto;

import java.time.Instant;

public record FeedbackResponse(
        Long id,
        String name,
        int rating,
        Instant createdAt,
        String status
) {}

