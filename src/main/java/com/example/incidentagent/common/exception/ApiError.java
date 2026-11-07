package com.example.incidentagent.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** The standard error envelope returned by every failing endpoint. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<Detail> details,
        String path) {

    /** One field-level problem, e.g. field "severity", issue "must not be null". */
    public record Detail(String field, String issue) {
    }
}
