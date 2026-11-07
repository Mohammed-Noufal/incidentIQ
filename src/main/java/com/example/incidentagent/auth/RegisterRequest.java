package com.example.incidentagent.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "[A-Za-z0-9._-]+", message = "must contain only letters, numbers, dots, underscores, or hyphens")
        String username,
        @NotBlank
        @Size(min = 12, max = 72, message = "must be between 12 and 72 characters")
        String password) {
}
