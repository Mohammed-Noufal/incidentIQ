package com.example.incidentagent.action;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionAttemptRepository extends JpaRepository<ActionAttempt, Long> {

    List<ActionAttempt> findByIncidentIdOrderBySequenceNoAsc(Long incidentId);

    Optional<ActionAttempt> findByIncidentIdAndSequenceNo(Long incidentId, int sequenceNo);
}
