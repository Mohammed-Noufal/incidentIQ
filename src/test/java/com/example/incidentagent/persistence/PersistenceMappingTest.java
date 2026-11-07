package com.example.incidentagent.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.action.ActionAttemptRepository;
import com.example.incidentagent.action.Outcome;
import com.example.incidentagent.alert.Alert;
import com.example.incidentagent.alert.AlertRepository;
import com.example.incidentagent.briefing.BriefingRecord;
import com.example.incidentagent.briefing.BriefingRecordRepository;
import com.example.incidentagent.briefing.BriefingStatus;
import com.example.incidentagent.catalog.MonitoredService;
import com.example.incidentagent.catalog.MonitoredServiceRepository;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentRepository;
import com.example.incidentagent.incident.IncidentSource;
import com.example.incidentagent.incident.IncidentStatus;
import com.example.incidentagent.incident.MemorySyncStatus;
import com.example.incidentagent.incident.Severity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Checks that every entity maps onto the Flyway-created schema (Hibernate does not validate it,
 * so these round-trips are what catch a column-name or type mismatch).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class PersistenceMappingTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private MonitoredServiceRepository serviceRepository;
    @Autowired
    private IncidentRepository incidentRepository;
    @Autowired
    private AlertRepository alertRepository;
    @Autowired
    private ActionAttemptRepository actionAttemptRepository;
    @Autowired
    private BriefingRecordRepository briefingRecordRepository;

    private MonitoredService newService(String name) {
        return em.persist(new MonitoredService(name, "test service " + name));
    }

    private Incident newIncident(MonitoredService service, String code) {
        Incident incident = new Incident();
        incident.setCode(code);
        incident.setService(service);
        incident.setTitle("Checkout latency above 8s");
        incident.setSymptoms("p95 latency 8.4s; connection not available");
        incident.setSeverity(Severity.HIGH);
        return em.persist(incident);
    }

    @Test
    void savesAndReadsBackAFullIncidentGraph() {
        MonitoredService payments = newService("payments-worker");
        MonitoredService cache = newService("cache-service");
        Incident incident = newIncident(payments, "INC-001");

        Alert alert = new Alert();
        alert.setIncident(incident);
        alert.setService(payments);
        alert.setSeverity(Severity.HIGH);
        alert.setTitle("Checkout latency above 8s");
        alert.setMessage("p95 checkout latency 8.4s for 6 minutes");
        alert.setLogExcerpt("HikariPool-1 - Connection is not available");
        em.persist(alert);

        ActionAttempt first = new ActionAttempt();
        first.setIncident(incident);
        first.setSequenceNo(1);
        first.setActionDescription("Restarted cache-service");
        first.setTargetService(cache);
        first.setOutcome(Outcome.WORSENED);
        first.setObservation("Latency doubled within 90 seconds");
        first.setExplanation("The restart emptied the cache, so every request hit the saturated database");
        em.persist(first);

        ActionAttempt second = new ActionAttempt();
        second.setIncident(incident);
        second.setSequenceNo(2);
        second.setActionDescription("Terminated the slow report query");
        em.persist(second); // outcome not recorded yet

        em.flush();
        em.clear();

        Incident loaded = incidentRepository.findByCode("INC-001").orElseThrow();
        assertThat(loaded.getId()).isNotNull();
        assertThat(loaded.getService().getName()).isEqualTo("payments-worker");
        assertThat(loaded.getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(loaded.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(loaded.getSource()).isEqualTo(IncidentSource.LIVE);
        assertThat(loaded.getMemorySyncStatus()).isEqualTo(MemorySyncStatus.PENDING);
        assertThat(loaded.getOpenedAt()).isNotNull();
        assertThat(loaded.getResolvedAt()).isNull();

        List<Alert> alerts = alertRepository.findByIncidentIdOrderByReceivedAtAsc(loaded.getId());
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getReceivedAt()).isNotNull();
        assertThat(alerts.get(0).getLogExcerpt()).contains("Connection is not available");

        List<ActionAttempt> actions = actionAttemptRepository.findByIncidentIdOrderBySequenceNoAsc(loaded.getId());
        assertThat(actions).extracting(ActionAttempt::getSequenceNo).containsExactly(1, 2);
        assertThat(actions.get(0).getOutcome()).isEqualTo(Outcome.WORSENED);
        assertThat(actions.get(0).getExplanation()).contains("emptied the cache");
        assertThat(actions.get(0).getTargetService().getName()).isEqualTo("cache-service");
        assertThat(actions.get(1).getOutcome()).isNull();
        assertThat(actions.get(1).getPerformedAt()).isNotNull();

        assertThat(actionAttemptRepository.findByIncidentIdAndSequenceNo(loaded.getId(), 2)).isPresent();
        assertThat(actionAttemptRepository.findByIncidentIdAndSequenceNo(loaded.getId(), 99)).isEmpty();
        assertThat(incidentRepository.findByStatusOrderByOpenedAtDesc(IncidentStatus.OPEN)).hasSize(1);
        assertThat(incidentRepository.findByStatusOrderByOpenedAtDesc(IncidentStatus.RESOLVED)).isEmpty();
    }

    @Test
    void savesAndReadsBackABriefingRecord() {
        Incident incident = newIncident(newService("orders-api"), "INC-002");

        BriefingRecord record = new BriefingRecord();
        record.setIncident(incident);
        record.setStatus(BriefingStatus.DEGRADED);
        record.setRecallQuery("orders-api slow responses, database connection wait timeouts");
        record.setLatencyMsRecall(420);
        em.persist(record);
        em.flush();
        em.clear();

        BriefingRecord loaded = briefingRecordRepository
                .findFirstByIncidentIdOrderByCreatedAtDesc(incident.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(BriefingStatus.DEGRADED);
        assertThat(loaded.getRecallQuery()).contains("orders-api");
        assertThat(loaded.getLatencyMsRecall()).isEqualTo(420);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getBriefingJson()).isNull();
    }

    @Test
    void serviceNamesMustBeUnique() {
        serviceRepository.saveAndFlush(new MonitoredService("dup-service", "first"));
        assertThatThrownBy(() -> serviceRepository.saveAndFlush(new MonitoredService("dup-service", "second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void actionSequenceNumbersMustBeUniquePerIncident() {
        Incident incident = newIncident(newService("payments-worker"), "INC-003");
        ActionAttempt first = new ActionAttempt();
        first.setIncident(incident);
        first.setSequenceNo(1);
        first.setActionDescription("first");
        actionAttemptRepository.saveAndFlush(first);

        ActionAttempt duplicate = new ActionAttempt();
        duplicate.setIncident(incident);
        duplicate.setSequenceNo(1);
        duplicate.setActionDescription("duplicate sequence");
        assertThatThrownBy(() -> actionAttemptRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
