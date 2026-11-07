package com.example.incidentagent.auth;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username) {
}
