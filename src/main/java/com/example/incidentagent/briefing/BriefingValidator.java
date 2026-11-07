package com.example.incidentagent.briefing;

import com.example.incidentagent.action.ActionAttempt;
import com.example.incidentagent.action.ActionAttemptRepository;
import com.example.incidentagent.action.Outcome;
import com.example.incidentagent.briefing.dto.BriefingContent;
import com.example.incidentagent.briefing.dto.BriefingContent.CitedAction;
import com.example.incidentagent.briefing.dto.BriefingContent.LikelyCause;
import com.example.incidentagent.briefing.dto.BriefingContent.PrecedentAssessment;
import com.example.incidentagent.briefing.dto.BriefingContent.RecommendedAction;
import com.example.incidentagent.briefing.dto.BriefingContent.WarningEvidence;
import com.example.incidentagent.briefing.dto.BriefingContent.WarningItem;
import com.example.incidentagent.briefing.dto.ValidationResult;
import com.example.incidentagent.briefing.dto.ValidationResult.RemovedWarning;
import com.example.incidentagent.incident.Incident;
import com.example.incidentagent.incident.IncidentRepository;
import com.example.incidentagent.memory.RecalledMemory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates LLM reasoning against MySQL facts and Hindsight retrieved memories.
 * Enforces rules V1-V5 and attaches authoritative MySQL evidence to surviving warnings (C4).
 */
@Component
public class BriefingValidator {

    private static final Logger log = LoggerFactory.getLogger(BriefingValidator.class);

    private final IncidentRepository incidentRepository;
    private final ActionAttemptRepository actionAttemptRepository;

    public BriefingValidator(IncidentRepository incidentRepository,
                             ActionAttemptRepository actionAttemptRepository) {
        this.incidentRepository = incidentRepository;
        this.actionAttemptRepository = actionAttemptRepository;
    }

    public record ValidatedBriefing(
            BriefingContent content,
            ValidationResult validationResult
    ) {}

    public ValidatedBriefing validate(BriefingContent raw, List<RecalledMemory> retrievedMemories) {
        if (raw == null) {
            return new ValidatedBriefing(null, new ValidationResult(false, List.of()));
        }

        Set<String> retrievedCodes = new HashSet<>();
        if (retrievedMemories != null) {
            for (RecalledMemory m : retrievedMemories) {
                if (m.incidentCode() != null && !m.incidentCode().isBlank()) {
                    retrievedCodes.add(m.incidentCode().trim().toUpperCase());
                }
            }
        }

        List<RemovedWarning> removedWarnings = new ArrayList<>();
        boolean noPrecedent = raw.noRelevantPrecedent();

        // V2 & V5: Clean precedent assessments
        List<PrecedentAssessment> cleanAssessments = new ArrayList<>();
        if (raw.precedentAssessment() != null) {
            for (PrecedentAssessment pa : raw.precedentAssessment()) {
                if (pa.incidentCode() != null && retrievedCodes.contains(pa.incidentCode().trim().toUpperCase())) {
                    cleanAssessments.add(pa);
                }
            }
        }

        // V2 & V5: Filter citations in likely causes
        List<LikelyCause> cleanCauses = new ArrayList<>();
        if (raw.likelyCauses() != null) {
            for (LikelyCause lc : raw.likelyCauses()) {
                List<String> validCitations = Collections.emptyList();
                if (!noPrecedent && lc.citedIncidents() != null) {
                    validCitations = lc.citedIncidents().stream()
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .filter(retrievedCodes::contains)
                            .toList();
                }
                cleanCauses.add(new LikelyCause(lc.text(), validCitations));
            }
        }

        // V2 & V5: Filter citations in recommended actions
        List<RecommendedAction> cleanActions = new ArrayList<>();
        if (raw.recommendedActions() != null) {
            for (RecommendedAction ra : raw.recommendedActions()) {
                List<String> validCitations = Collections.emptyList();
                if (!noPrecedent && ra.citedIncidents() != null) {
                    validCitations = ra.citedIncidents().stream()
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .filter(retrievedCodes::contains)
                            .toList();
                }
                cleanActions.add(new RecommendedAction(ra.text(), validCitations));
            }
        }

        // V2, V3, V4, V5: Validate warnings
        List<WarningItem> survivingWarnings = new ArrayList<>();
        if (raw.warnings() != null) {
            for (WarningItem w : raw.warnings()) {
                CitedAction ca = w.citedAction();
                if (ca == null || ca.incidentCode() == null || ca.incidentCode().isBlank()) {
                    removedWarnings.add(new RemovedWarning(w.action(), null, null, "Warning missing citedAction"));
                    continue;
                }

                String code = ca.incidentCode().trim().toUpperCase();
                int seq = ca.actionSequence();

                // Rule V5: If noRelevantPrecedent is true, no warnings are allowed
                if (noPrecedent) {
                    removedWarnings.add(new RemovedWarning(w.action(), code, seq,
                            "Stripped because noRelevantPrecedent is true"));
                    continue;
                }

                // Rule V2: Cited incident must be in retrieved set
                if (!retrievedCodes.contains(code)) {
                    removedWarnings.add(new RemovedWarning(w.action(), code, seq,
                            "Cited incident was not present in retrieved memories"));
                    continue;
                }

                // Rule V3: Authoritative check against MySQL action_attempt outcome == WORSENED
                Optional<Incident> incOpt = incidentRepository.findByCode(code);
                if (incOpt.isEmpty()) {
                    removedWarnings.add(new RemovedWarning(w.action(), code, seq,
                            "Incident " + code + " not found in database"));
                    continue;
                }

                Incident citedIncident = incOpt.get();
                Optional<ActionAttempt> actionOpt = actionAttemptRepository.findByIncidentIdAndSequenceNo(citedIncident.getId(), seq);
                if (actionOpt.isEmpty()) {
                    removedWarnings.add(new RemovedWarning(w.action(), code, seq,
                            "Action sequence " + seq + " for " + code + " not found in database"));
                    continue;
                }

                ActionAttempt actionAttempt = actionOpt.get();
                if (actionAttempt.getOutcome() != Outcome.WORSENED) {
                    removedWarnings.add(new RemovedWarning(w.action(), code, seq,
                            "Action outcome was " + actionAttempt.getOutcome() + ", not WORSENED in database"));
                    continue;
                }

                // Attach authoritative MySQL evidence
                Instant recordedAt = actionAttempt.getOutcomeRecordedAt() != null
                        ? actionAttempt.getOutcomeRecordedAt()
                        : actionAttempt.getPerformedAt();
                WarningEvidence evidence = new WarningEvidence(
                        "mysql",
                        "WORSENED",
                        recordedAt != null ? recordedAt.toString() : Instant.now().toString()
                );

                survivingWarnings.add(w.withEvidence(evidence));
            }
        }

        BriefingContent validatedContent = new BriefingContent(
                raw.source(),
                raw.model(),
                cleanAssessments,
                noPrecedent,
                raw.summary(),
                cleanCauses,
                cleanActions,
                survivingWarnings,
                raw.gaps() != null ? raw.gaps() : List.of()
        );

        ValidationResult validationResult = new ValidationResult(removedWarnings.isEmpty(), removedWarnings);
        return new ValidatedBriefing(validatedContent, validationResult);
    }
}

