package com.example.incidentagent.common.exception;

/** Unknown incident, service or action. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
