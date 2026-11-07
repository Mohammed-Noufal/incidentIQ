package com.example.incidentagent.seed;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.action.ActionAttemptRepository;
import com.example.incidentagent.catalog.MonitoredService;
import com.example.incidentagent.catalog.MonitoredServiceRepository;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentRepository;
import com.example.incidentagent.incident.IncidentSource;
import com.example.incidentagent.incident.IncidentStatus;
import com.example.incidentagent.incident.MemorySyncStatus;
import com.example.incidentagent.memory.IncidentMemory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedLoader {

    private static final Logger log = LoggerFactory.getLogger(SeedLoader.class);

    private final MonitoredServiceRepository serviceRepository;
    private final IncidentRepository incidentRepository;
    private final ActionAttemptRepository actionRepository;
    private final IncidentMemory incidentMemory;
    private final SeedGuard seedGuard;
    private final ObjectMapper objectMapper;

    public record SeedResult(
            int incidentsProcessed,
            int incidentsCreated,
            int actionsCreated,
            int memoryRetained,
            String status
    ) {}

    public SeedLoader(MonitoredServiceRepository serviceRepository,
                      IncidentRepository incidentRepository,
                      ActionAttemptRepository actionRepository,
                      IncidentMemory incidentMemory,
                      SeedGuard seedGuard,
                      ObjectMapper objectMapper) {
        this.serviceRepository = serviceRepository;
        this.incidentRepository = incidentRepository;
        this.actionRepository = actionRepository;
        this.incidentMemory = incidentMemory;
        this.seedGuard = seedGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SeedResult loadSeedData() {
        List<SeedIncidentDto> seedList = readSeedFile();
        seedGuard.validate(seedList);

        int created = 0;
        int actionsCount = 0;
        int retained = 0;

        for (SeedIncidentDto dto : seedList) {
            if (incidentRepository.findByCode(dto.code()).isPresent()) {
                continue;
            }

            MonitoredService service = serviceRepository.findByName(dto.serviceName())
                    .orElseGet(() -> serviceRepository.save(new MonitoredService(dto.serviceName(), "Auto-provisioned for seed data")));

            Incident incident = new Incident();
            incident.setCode(dto.code());
            incident.setService(service);
            incident.setTitle(dto.title());
            incident.setSymptoms(dto.symptoms());
            incident.setSeverity(dto.severity());
            incident.setStatus(IncidentStatus.RESOLVED);
            incident.setSource(IncidentSource.SEEDED);
            incident.setFailureFamily(dto.failureFamily());
            incident.setRootCause(dto.rootCause());
            incident.setLesson(dto.lesson());
            incident.setOpenedAt(Instant.now().minusSeconds(86400 * 7));
            incident.setResolvedAt(Instant.now().minusSeconds(86400 * 7 - 3600));
            incident.setMemorySyncStatus(MemorySyncStatus.PENDING);
            incident = incidentRepository.save(incident);

            List<ActionAttempt> actionList = new ArrayList<>();
            if (dto.actions() != null) {
                for (SeedIncidentDto.SeedActionDto actionDto : dto.actions()) {
                    MonitoredService target = null;
                    if (actionDto.targetService() != null && !actionDto.targetService().isBlank()) {
                        target = serviceRepository.findByName(actionDto.targetService())
                                .orElseGet(() -> serviceRepository.save(new MonitoredService(actionDto.targetService(), "Auto-provisioned for action target")));
                    }

                    ActionAttempt action = new ActionAttempt();
                    action.setIncident(incident);
                    action.setSequenceNo(actionDto.sequenceNo());
                    action.setActionDescription(actionDto.actionDescription());
                    action.setTargetService(target);
                    action.setOutcome(actionDto.outcome());
                    action.setObservation(actionDto.observation());
                    action.setExplanation(actionDto.explanation());
                    action.setPerformedAt(incident.getOpenedAt().plusSeconds(600L * actionDto.sequenceNo()));
                    action.setOutcomeRecordedAt(action.getPerformedAt().plusSeconds(300));
                    action = actionRepository.save(action);
                    actionList.add(action);
                    actionsCount++;
                }
            }

            // Normal retain path for seed data (D-17)
            try {
                incidentMemory.retainIncident(incident, actionList);
                incident.setMemorySyncStatus(MemorySyncStatus.SYNCED);
                incident.setMemorySyncedAt(Instant.now());
                incidentRepository.save(incident);
                retained++;
            } catch (Exception ex) {
                log.warn("Memory retain skipped/failed for seed incident {}: {}", dto.code(), ex.getMessage());
                incident.setMemorySyncStatus(MemorySyncStatus.FAILED);
                incidentRepository.save(incident);
            }

            created++;
        }

        return new SeedResult(seedList.size(), created, actionsCount, retained, "SUCCESS");
    }

    private List<SeedIncidentDto> readSeedFile() {
        try {
            ClassPathResource resource = new ClassPathResource("seed/incidents.json");
            try (InputStream in = resource.getInputStream()) {
                return objectMapper.readValue(in, new TypeReference<List<SeedIncidentDto>>() {});
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read seed file 'seed/incidents.json': " + ex.getMessage(), ex);
        }
    }
}

