package com.example.incidentagent.alert;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByIncidentIdOrderByReceivedAtAsc(Long incidentId);
}
