package com.example.incidentagent.briefing;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BriefingRecordRepository extends JpaRepository<BriefingRecord, Long> {

    Optional<BriefingRecord> findFirstByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
