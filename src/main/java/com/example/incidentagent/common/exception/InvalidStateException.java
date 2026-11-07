package com.example.incidentagent.common.exception;

/** Operation not allowed in the current state, e.g. resolving with unrecorded outcomes. */
public class InvalidStateException extends ApiException {

    public InvalidStateException(String message) {
        super(ErrorCode.INVALID_STATE, message);
    }
}
