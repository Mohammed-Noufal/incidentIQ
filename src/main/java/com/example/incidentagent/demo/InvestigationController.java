package com.example.incidentagent.demo;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class InvestigationController {
    private final IncidentInvestigationService service;
    public InvestigationController(IncidentInvestigationService service) { this.service = service; }
    @PostMapping("/investigate")
    public InvestigationResponse investigate(@Valid @RequestBody InvestigationRequest request) {
        return service.investigate(request);
    }
}
