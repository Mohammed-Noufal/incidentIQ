package com.example.incidentagent.action;

import com.example.incidentagent.action.dto.ActionResponse;
import com.example.incidentagent.action.dto.CreateActionRequest;
import com.example.incidentagent.action.dto.OutcomeResponse;
import com.example.incidentagent.action.dto.RecordOutcomeRequest;
import com.example.incidentagent.catalog.MonitoredService;
import com.example.incidentagent.catalog.MonitoredServiceRepository;
import com.example.incidentagent.common.exception.ApiError;
import com.example.incidentagent.common.exception.ApiException;
import com.example.incidentagent.common.exception.ErrorCode;
import com.example.incidentagent.common.exception.InvalidStateException;
import com.example.incidentagent.common.exception.NotFoundException;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentRepository;
import com.example.incidentagent.incident.IncidentStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionAttemptService {

    private final IncidentRepository incidentRepository;
    private final ActionAttemptRepository actionRepository;
    private final MonitoredServiceRepository serviceRepository;

    public ActionAttemptService(IncidentRepository incidentRepository,
                                ActionAttemptRepository actionRepository,
                                MonitoredServiceRepository serviceRepository) {
        this.incidentRepository = incidentRepository;
        this.actionRepository = actionRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional
    public ActionResponse recordAction(String incidentCode, CreateActionRequest request) {
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new NotFoundException("Incident '" + incidentCode + "' not found"));

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new InvalidStateException("Cannot add action to an already resolved incident");
        }

        List<ActionAttempt> existing = actionRepository.findByIncidentIdOrderBySequenceNoAsc(incident.getId());
        int nextSeq = existing.size() + 1;

        MonitoredService targetService = null;
        if (request.targetService() != null && !request.targetService().isBlank()) {
            targetService = serviceRepository.findByName(request.targetService()).orElse(null);
        }

        ActionAttempt action = new ActionAttempt();
        action.setIncident(incident);
        action.setSequenceNo(nextSeq);
        action.setActionDescription(request.actionDescription());
        action.setTargetService(targetService);
        action.setPerformedAt(Instant.now());
        action = actionRepository.save(action);

        return ActionResponse.from(action);
    }

    @Transactional
    public OutcomeResponse recordOutcome(String incidentCode, int sequenceNo, RecordOutcomeRequest request) {
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new NotFoundException("Incident '" + incidentCode + "' not found"));

        ActionAttempt action = actionRepository.findByIncidentIdAndSequenceNo(incident.getId(), sequenceNo)
                .orElseThrow(() -> new NotFoundException("Action sequence " + sequenceNo + " for incident '" + incidentCode + "' not found"));

        if (request.outcome() == Outcome.WORSENED) {
            if (request.explanation() == null || request.explanation().isBlank()) {
                throw new ApiException(
                        ErrorCode.VALIDATION_FAILED,
                        "Request validation failed",
                        List.of(new ApiError.Detail("explanation", "explanation is required when outcome is WORSENED"))
                );
            }
        }

        action.setOutcome(request.outcome());
        action.setObservation(request.observation());
        action.setExplanation(request.explanation());
        action.setOutcomeRecordedAt(Instant.now());
        action = actionRepository.save(action);

        return new OutcomeResponse(
                incident.getCode(),
                action.getSequenceNo(),
                action.getOutcome(),
                action.getOutcomeRecordedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ActionResponse> getActionsForIncident(Long incidentId) {
        return actionRepository.findByIncidentIdOrderBySequenceNoAsc(incidentId)
                .stream()
                .map(ActionResponse::from)
                .toList();
    }
}

