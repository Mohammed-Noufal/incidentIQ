package com.example.incidentagent.incident;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.action.ActionAttemptRepository;
import com.example.incidentagent.action.dto.ActionResponse;
import com.example.incidentagent.alert.Alert;
import com.example.incidentagent.alert.AlertRepository;
import com.example.incidentagent.common.exception.InvalidStateException;
import com.example.incidentagent.common.exception.NotFoundException;
import com.example.incidentagent.incident.dto.IncidentDetailResponse;
import com.example.incidentagent.incident.dto.IncidentDetailResponse.AlertSummary;
import com.example.incidentagent.incident.dto.IncidentResponse;
import com.example.incidentagent.incident.dto.ResolveIncidentRequest;
import com.example.incidentagent.incident.dto.ResolveIncidentResponse;
import com.example.incidentagent.incident.dto.ResolveIncidentResponse.MemorySyncInfo;
import com.example.incidentagent.memory.IncidentMemory;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

    private final IncidentRepository incidentRepository;
    private final ActionAttemptRepository actionRepository;
    private final AlertRepository alertRepository;
    private final IncidentMemory incidentMemory;

    public IncidentService(IncidentRepository incidentRepository,
                           ActionAttemptRepository actionRepository,
                           AlertRepository alertRepository,
                           IncidentMemory incidentMemory) {
        this.incidentRepository = incidentRepository;
        this.actionRepository = actionRepository;
        this.alertRepository = alertRepository;
        this.incidentMemory = incidentMemory;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listIncidents(IncidentStatus status, String serviceName) {
        List<Incident> all;
        if (status != null) {
            all = incidentRepository.findByStatusOrderByOpenedAtDesc(status);
        } else {
            all = incidentRepository.findAll();
            all.sort(Comparator.comparing(Incident::getOpenedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        if (serviceName != null && !serviceName.isBlank()) {
            all = all.stream()
                    .filter(i -> i.getService() != null && serviceName.equalsIgnoreCase(i.getService().getName()))
                    .toList();
        }

        return all.stream().map(IncidentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse getIncidentDetail(String code) {
        Incident incident = incidentRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Incident '" + code + "' not found"));

        List<ActionResponse> actions = actionRepository.findByIncidentIdOrderBySequenceNoAsc(incident.getId())
                .stream()
                .map(ActionResponse::from)
                .toList();

        List<AlertSummary> alerts = alertRepository.findByIncidentIdOrderByReceivedAtAsc(incident.getId())
                .stream()
                .map(a -> new AlertSummary(
                        a.getId(),
                        a.getSeverity() != null ? a.getSeverity().name() : null,
                        a.getTitle(),
                        a.getMessage(),
                        a.getLogExcerpt(),
                        a.getReceivedAt()
                ))
                .toList();

        return IncidentDetailResponse.from(incident, actions, alerts);
    }

    @Transactional
    public ResolveIncidentResponse resolveIncident(String code, ResolveIncidentRequest request) {
        Incident incident = incidentRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Incident '" + code + "' not found"));

        if (incident.getStatus() == IncidentStatus.RESOLVED) {
            throw new InvalidStateException("Incident '" + code + "' is already resolved");
        }

        List<ActionAttempt> actions = actionRepository.findByIncidentIdOrderBySequenceNoAsc(incident.getId());
        for (ActionAttempt action : actions) {
            if (action.getOutcome() == null) {
                throw new InvalidStateException("Cannot resolve incident while action " + action.getSequenceNo()
                        + " has no recorded outcome");
            }
        }

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(Instant.now());
        incident.setRootCause(request.rootCause());
        incident.setLesson(request.lesson());

        // Attempt memory retain
        try {
            incidentMemory.retainIncident(incident, actions);
            incident.setMemorySyncStatus(MemorySyncStatus.SYNCED);
            incident.setMemorySyncedAt(Instant.now());
            log.info("Incident {} successfully retained to memory on resolve", code);
        } catch (Exception ex) {
            log.warn("Memory retain failed on resolve for {}: {}", code, ex.getMessage());
            incident.setMemorySyncStatus(MemorySyncStatus.FAILED);
        }

        incident = incidentRepository.save(incident);

        return new ResolveIncidentResponse(
                incident.getCode(),
                incident.getStatus(),
                incident.getResolvedAt(),
                new MemorySyncInfo(incident.getMemorySyncStatus(), incident.getCode())
        );
    }

    @Transactional
    public ResolveIncidentResponse retainMemory(String code) {
        Incident incident = incidentRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Incident '" + code + "' not found"));

        List<ActionAttempt> actions = actionRepository.findByIncidentIdOrderBySequenceNoAsc(incident.getId());

        try {
            incidentMemory.retainIncident(incident, actions);
            incident.setMemorySyncStatus(MemorySyncStatus.SYNCED);
            incident.setMemorySyncedAt(Instant.now());
        } catch (Exception ex) {
            log.warn("Memory re-sync failed for {}: {}", code, ex.getMessage());
            incident.setMemorySyncStatus(MemorySyncStatus.FAILED);
            throw ex;
        }

        incident = incidentRepository.save(incident);

        return new ResolveIncidentResponse(
                incident.getCode(),
                incident.getStatus(),
                incident.getResolvedAt(),
                new MemorySyncInfo(incident.getMemorySyncStatus(), incident.getCode())
        );
    }
}

