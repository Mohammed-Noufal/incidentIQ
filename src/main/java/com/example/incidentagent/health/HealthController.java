package com.example.incidentagent.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    /** 200 when database is reachable, 503 otherwise. */
    @GetMapping({"/api/health", "/health"})
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = healthService.check();
        int status = HealthService.UP.equals(response.status()) ? 200 : 503;
        return ResponseEntity.status(status).body(response);
    }
}
