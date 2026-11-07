package com.example.incidentagent.alert;

import com.example.incidentagent.alert.dto.CreateAlertRequest;
import com.example.incidentagent.alert.dto.CreateAlertResponse;
import com.example.incidentagent.alert.dto.CreateAlertResponse.IncidentSummary;
import com.example.incidentagent.catalog.MonitoredService;
import com.example.incidentagent.catalog.MonitoredServiceRepository;
import com.example.incidentagent.common.exception.NotFoundException;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentCodeGenerator;
import com.example.incidentagent.incident.IncidentRepository;
import com.example.incidentagent.incident.IncidentSource;
import com.example.incidentagent.incident.IncidentStatus;
import com.example.incidentagent.incident.MemorySyncStatus;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {

    private final MonitoredServiceRepository serviceRepository;
    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final IncidentCodeGenerator codeGenerator;

    public AlertService(MonitoredServiceRepository serviceRepository,
                        IncidentRepository incidentRepository,
                        AlertRepository alertRepository,
                        IncidentCodeGenerator codeGenerator) {
        this.serviceRepository = serviceRepository;
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.codeGenerator = codeGenerator;
    }

    @Transactional
    public CreateAlertResponse ingestAlert(CreateAlertRequest request) {
        MonitoredService service = serviceRepository.findByName(request.serviceName())
                .orElseThrow(() -> new NotFoundException("Service '" + request.serviceName() + "' not found"));

        String code = codeGenerator.nextCode();

        Incident incident = new Incident();
        incident.setCode(code);
        incident.setService(service);
        incident.setTitle(request.title());
        incident.setSymptoms(request.message());
        incident.setSeverity(request.severity());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSource(IncidentSource.LIVE);
        incident.setMemorySyncStatus(MemorySyncStatus.PENDING);
        incident.setOpenedAt(Instant.now());
        incident = incidentRepository.save(incident);

        Alert alert = new Alert();
        alert.setIncident(incident);
        alert.setService(service);
        alert.setSeverity(request.severity());
        alert.setTitle(request.title());
        alert.setMessage(request.message());
        alert.setLogExcerpt(request.logExcerpt());
        alert.setReceivedAt(Instant.now());
        alert = alertRepository.save(alert);

        return new CreateAlertResponse(
                alert.getId(),
                new IncidentSummary(
                        incident.getCode(),
                        incident.getStatus(),
                        service.getName(),
                        incident.getSeverity(),
                        incident.getOpenedAt()
                )
        );
    }
}

