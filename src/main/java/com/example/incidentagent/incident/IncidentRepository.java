package com.example.incidentagent.incident;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByCode(String code);

    List<Incident> findByStatusOrderByOpenedAtDesc(IncidentStatus status);
}
