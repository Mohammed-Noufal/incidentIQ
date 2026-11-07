package com.example.incidentagent.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, Long> {

    Optional<MonitoredService> findByName(String name);

    List<MonitoredService> findAllByOrderByNameAsc();
}
