package com.example.incidentagent.seed;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.incidentagent.incident.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeedGuardTest {

    private final SeedGuard seedGuard = new SeedGuard();

    @Test
    void allowsSeedIncidentsWithDifferentFailureFamilies() {
        SeedIncidentDto inc = new SeedIncidentDto(
                "INC-001",
                "cache-service",
                "Redis OOM",
                "Out of memory",
                Severity.HIGH,
                "redis-memory-exhaustion",
                "Policy issue",
                "Check policy",
                List.of()
        );

        assertThatCode(() -> seedGuard.validate(List.of(inc))).doesNotThrowAnyException();
    }

    @Test
    void rejectsSeedIncidentsCarryingHeldOutFailureFamily() {
        SeedIncidentDto inc = new SeedIncidentDto(
                "INC-099",
                "payments-worker",
                "Connection pool",
                "Pool exhausted",
                Severity.HIGH,
                "connection-pool-exhaustion",
                "Slow report query",
                "Do not restart cache",
                List.of()
        );

        assertThatThrownBy(() -> seedGuard.validate(List.of(inc)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seed guard violation")
                .hasMessageContaining("connection-pool-exhaustion");
    }
}

