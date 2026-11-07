package com.example.incidentagent.demo;

import com.example.incidentagent.integration.groq.GroqClient;
import com.example.incidentagent.integration.hindsight.HindsightMemoryClient;
import org.springframework.stereotype.Service;

@Service
public class IncidentInvestigationService {
    private final GroqClient groq;
    private final HindsightMemoryClient hindsight;

    public IncidentInvestigationService(GroqClient groq, HindsightMemoryClient hindsight) {
        this.groq = groq;
        this.hindsight = hindsight;
    }

    public InvestigationResponse investigate(InvestigationRequest request) {
        String query = "Service: " + request.serviceName() + "\nIncident: " + request.title()
                + "\nSeverity: " + value(request.severity(), "MEDIUM") + "\nSymptoms: " + request.symptoms();
        String memories;
        try {
            memories = hindsight.recall(query);
        } catch (Exception ex) {
            memories = "Memory recall unavailable: " + safeMessage(ex);
        }
        String system = "You are IncidentIQ, an incident response assistant. Analyze the incident using the supplied historical memory context. Clearly separate evidence, hypotheses, and recommended safe diagnostic steps. Never claim certainty without evidence. Do not recommend destructive actions. Return concise Markdown with: Summary, Likely causes, Recommended checks, Risks, Confidence.";
        String analysis;
        try {
            analysis = groq.analyze(system, "CURRENT INCIDENT\n" + query + "\n\nHISTORICAL MEMORY CONTEXT\n" + memories);
        } catch (Exception ex) {
            analysis = "### Advisory Notice\nAI reasoning is currently in degraded mode: " + safeMessage(ex) + ".\n\nPlease refer to the historical memory context below for relevant past incidents.";
        }
        boolean saved = false;
        String warning = null;
        try {
            hindsight.retain("Resolved/observed incident record for " + request.serviceName() + ": "
                    + request.title() + ". Symptoms: " + request.symptoms() + ". Severity: "
                    + value(request.severity(), "MEDIUM") + ". AI analysis: " + analysis);
            saved = true;
        } catch (Exception ex) {
            warning = safeMessage(ex);
        }
        return new InvestigationResponse(analysis, memories, saved, warning);
    }

    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null) return "External memory service unavailable";
        return message.replaceAll("(?i)Bearer\\s+[^\\s]+", "Bearer [REDACTED]");
    }
}
