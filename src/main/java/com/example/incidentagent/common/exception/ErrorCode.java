package com.example.incidentagent.common.exception;

/** Machine-readable error codes returned in the standard error envelope, with their HTTP status. */
public enum ErrorCode {
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    INVALID_STATE(409),
    UNSUPPORTED_MEDIA_TYPE(415),
    VALIDATION_FAILED(422),
    INTERNAL_ERROR(500),
    MEMORY_UNAVAILABLE(502),
    LLM_UNAVAILABLE(502);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }

    public int status() {
        return status;
    }
}
