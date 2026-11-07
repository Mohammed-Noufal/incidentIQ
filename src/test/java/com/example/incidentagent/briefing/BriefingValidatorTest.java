package com.example.incidentagent.briefing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.action.ActionAttemptRepository;
import com.example.incidentagent.action.Outcome;
import com.example.incidentagent.briefing.BriefingValidator.ValidatedBriefing;
import com.example.incidentagent.briefing.dto.BriefingContent;
import com.example.incidentagent.briefing.dto.BriefingContent.CitedAction;
import com.example.incidentagent.briefing.dto.BriefingContent.LikelyCause;
import com.example.incidentagent.briefing.dto.BriefingContent.RecommendedAction;
import com.example.incidentagent.briefing.dto.BriefingContent.WarningItem;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentRepository;
import com.example.incidentagent.memory.RecalledMemory;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BriefingValidatorTest {

    private IncidentRepository incidentRepository;
    private ActionAttemptRepository actionAttemptRepository;
    private BriefingValidator validator;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(IncidentRepository.class);
        actionAttemptRepository = mock(ActionAttemptRepository.class);
        validator = new BriefingValidator(incidentRepository, actionAttemptRepository);
    }

    @Test
    void attachesEvidenceToWarningWhenActionOutcomeIsWorsenedInMysql() {
        Incident inc = new Incident();
        inc.setCode("INC-019");

        ActionAttempt action = new ActionAttempt();
        action.setSequenceNo(1);
        action.setOutcome(Outcome.WORSENED);
        action.setOutcomeRecordedAt(Instant.parse("2026-11-05T02:31:00Z"));

        when(incidentRepository.findByCode("INC-019")).thenReturn(Optional.of(inc));
        when(actionAttemptRepository.findByIncidentIdAndSequenceNo(inc.getId(), 1)).thenReturn(Optional.of(action));

        BriefingContent content = new BriefingContent(
                "groq",
                "openai/gpt-oss-20b",
                List.of(),
                false,
                "Summary",
                List.of(),
                List.of(),
                List.of(new WarningItem("Restart cache", new CitedAction("INC-019", 1), "Made it worse", null)),
                List.of()
        );

        List<RecalledMemory> retrieved = List.of(new RecalledMemory(1, "INC-019", "Outcome: WORSENED", 0.9));

        ValidatedBriefing result = validator.validate(content, retrieved);

        assertThat(result.validationResult().passed()).isTrue();
        assertThat(result.content().warnings()).hasSize(1);
        WarningItem warning = result.content().warnings().get(0);
        assertThat(warning.evidence()).isNotNull();
        assertThat(warning.evidence().source()).isEqualTo("mysql");
        assertThat(warning.evidence().outcome()).isEqualTo("WORSENED");
        assertThat(warning.evidence().recordedAt()).isEqualTo("2026-11-05T02:31:00Z");
    }

    @Test
    void removesWarningWhenActionOutcomeIsNotWorsenedInMysql() {
        Incident inc = new Incident();
        inc.setCode("INC-019");

        ActionAttempt action = new ActionAttempt();
        action.setSequenceNo(1);
        action.setOutcome(Outcome.NO_EFFECT); // NOT WORSENED!

        when(incidentRepository.findByCode("INC-019")).thenReturn(Optional.of(inc));
        when(actionAttemptRepository.findByIncidentIdAndSequenceNo(inc.getId(), 1)).thenReturn(Optional.of(action));

        BriefingContent content = new BriefingContent(
                "groq",
                "openai/gpt-oss-20b",
                List.of(),
                false,
                "Summary",
                List.of(),
                List.of(),
                List.of(new WarningItem("Restart cache", new CitedAction("INC-019", 1), "Made it worse", null)),
                List.of()
        );

        List<RecalledMemory> retrieved = List.of(new RecalledMemory(1, "INC-019", "Outcome: NO_EFFECT", 0.9));

        ValidatedBriefing result = validator.validate(content, retrieved);

        assertThat(result.validationResult().passed()).isFalse();
        assertThat(result.content().warnings()).isEmpty();
        assertThat(result.validationResult().removedWarnings()).hasSize(1);
        assertThat(result.validationResult().removedWarnings().get(0).reason()).contains("not WORSENED in database");
    }

    @Test
    void removesWarningAndCitationsWhenNoRelevantPrecedentIsTrue() {
        BriefingContent content = new BriefingContent(
                "groq",
                "openai/gpt-oss-20b",
                List.of(),
                true, // NO RELEVANT PRECEDENT
                "Summary",
                List.of(new LikelyCause("Cause", List.of("INC-001"))),
                List.of(new RecommendedAction("Action", List.of("INC-001"))),
                List.of(new WarningItem("Restart cache", new CitedAction("INC-001", 1), "Made it worse", null)),
                List.of()
        );

        List<RecalledMemory> retrieved = List.of(new RecalledMemory(1, "INC-001", "Text", 0.5));

        ValidatedBriefing result = validator.validate(content, retrieved);

        assertThat(result.content().warnings()).isEmpty();
        assertThat(result.content().likelyCauses().get(0).citedIncidents()).isEmpty();
        assertThat(result.content().recommendedActions().get(0).citedIncidents()).isEmpty();
    }
}

