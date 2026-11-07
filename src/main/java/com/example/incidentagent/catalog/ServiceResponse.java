package com.example.incidentagent.catalog;

/** API representation of a service in the catalog. */
public record ServiceResponse(Long id, String name, String description) {

    public static ServiceResponse from(MonitoredService service) {
        return new ServiceResponse(service.getId(), service.getName(), service.getDescription());
    }
}
