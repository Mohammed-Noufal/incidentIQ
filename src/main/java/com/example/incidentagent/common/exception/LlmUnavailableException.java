package com.example.incidentagent.common.exception;

/** The LLM (Groq) failed and no degraded response is possible. */
public class LlmUnavailableException extends ApiException {

    public LlmUnavailableException(String message) {
        super(ErrorCode.LLM_UNAVAILABLE, message);
    }
}
