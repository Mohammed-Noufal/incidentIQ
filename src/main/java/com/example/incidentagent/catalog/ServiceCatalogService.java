package com.example.incidentagent.catalog;

import com.example.incidentagent.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only access to the service catalog. */
@Service
@Transactional(readOnly = true)
public class ServiceCatalogService {

    private final MonitoredServiceRepository repository;

    public ServiceCatalogService(MonitoredServiceRepository repository) {
        this.repository = repository;
    }

    public List<ServiceResponse> listAll() {
        return repository.findAllByOrderByNameAsc().stream().map(ServiceResponse::from).toList();
    }

    public ServiceResponse getByName(String name) {
        return repository.findByName(name)
                .map(ServiceResponse::from)
                .orElseThrow(() -> new NotFoundException("Service '" + name + "' not found"));
    }
}
