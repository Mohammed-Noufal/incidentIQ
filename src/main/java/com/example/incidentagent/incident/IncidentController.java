package com.example.incidentagent.incident;

import com.example.incidentagent.action.ActionAttemptService;
import com.example.incidentagent.action.dto.ActionResponse;
import com.example.incidentagent.action.dto.CreateActionRequest;
import com.example.incidentagent.action.dto.OutcomeResponse;
import com.example.incidentagent.action.dto.RecordOutcomeRequest;
import com.example.incidentagent.incident.dto.IncidentDetailResponse;
import com.example.incidentagent.incident.dto.IncidentResponse;
import com.example.incidentagent.incident.dto.ResolveIncidentRequest;
import com.example.incidentagent.incident.dto.ResolveIncidentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final ActionAttemptService actionService;

    public IncidentController(IncidentService incidentService, ActionAttemptService actionService) {
        this.incidentService = incidentService;
        this.actionService = actionService;
    }

    @GetMapping
    public List<IncidentResponse> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) String service) {
        return incidentService.listIncidents(status, service);
    }

    @GetMapping("/{id}")
    public IncidentDetailResponse get(@PathVariable("id") String code) {
        return incidentService.getIncidentDetail(code);
    }

    @PostMapping("/{id}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public ActionResponse recordAction(
            @PathVariable("id") String code,
            @Valid @RequestBody CreateActionRequest request) {
        return actionService.recordAction(code, request);
    }

    @PatchMapping("/{id}/actions/{seq}/outcome")
    public OutcomeResponse recordOutcome(
            @PathVariable("id") String code,
            @PathVariable("seq") int seq,
            @Valid @RequestBody RecordOutcomeRequest request) {
        return actionService.recordOutcome(code, seq, request);
    }

    @PostMapping("/{id}/resolve")
    public ResolveIncidentResponse resolve(
            @PathVariable("id") String code,
            @Valid @RequestBody ResolveIncidentRequest request) {
        return incidentService.resolveIncident(code, request);
    }

    @PostMapping("/{id}/memory/retain")
    public ResolveIncidentResponse retainMemory(@PathVariable("id") String code) {
        return incidentService.retainMemory(code);
    }
}

