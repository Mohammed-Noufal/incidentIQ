package com.example.incidentagent.health;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Status of one dependency. Never contains secrets. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComponentHealth(String status, Long latencyMs, String detail) {

    public static ComponentHealth up(long latencyMs) {
        return new ComponentHealth(HealthService.UP, latencyMs, null);
    }

    public static ComponentHealth down(String detail) {
        return new ComponentHealth(HealthService.DOWN, null, detail);
    }

    public static ComponentHealth configured(boolean isConfigured) {
        return new ComponentHealth(isConfigured ? HealthService.CONFIGURED : HealthService.NOT_CONFIGURED, null, null);
    }
}
