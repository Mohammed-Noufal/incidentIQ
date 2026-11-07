package com.example.incidentagent.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class ServiceCatalogController {

    private final ServiceCatalogService catalogService;

    public ServiceCatalogController(ServiceCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<ServiceResponse> list() {
        return catalogService.listAll();
    }

    @GetMapping("/{name}")
    public ServiceResponse get(@PathVariable String name) {
        return catalogService.getByName(name);
    }
}
