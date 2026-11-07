package com.example.incidentagent.common.exception;

/** Hindsight was unreachable or returned an error. */
public class MemoryUnavailableException extends ApiException {

    public MemoryUnavailableException(String message) {
        super(ErrorCode.MEMORY_UNAVAILABLE, message);
    }
}
