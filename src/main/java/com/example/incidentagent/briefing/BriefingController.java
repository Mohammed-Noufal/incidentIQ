package com.example.incidentagent.briefing;

import com.example.incidentagent.briefing.dto.BriefingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class BriefingController {

    private final BriefingOrchestrator orchestrator;

    public BriefingController(BriefingOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/{id}/briefing")
    public BriefingResponse generateBriefing(@PathVariable("id") String incidentCode) {
        return orchestrator.generateBriefing(incidentCode);
    }

    @GetMapping("/{id}/briefing")
    public BriefingResponse getLatestBriefing(@PathVariable("id") String incidentCode) {
        return orchestrator.getLatestBriefing(incidentCode);
    }
}

