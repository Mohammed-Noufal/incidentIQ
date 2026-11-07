package com.example.incidentagent.common.exception;

import java.util.List;

/** Base class for expected, client-visible failures. The message is safe to show to the caller. */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ApiError.Detail> details;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public ApiException(ErrorCode errorCode, String message, List<ApiError.Detail> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ApiError.Detail> getDetails() {
        return details;
    }
}
