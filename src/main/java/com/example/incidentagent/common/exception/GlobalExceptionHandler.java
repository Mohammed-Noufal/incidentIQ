package com.example.incidentagent.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Maps every exception to the standard {@link ApiError} envelope.
 * Unexpected errors are logged in full but never leak details (or stack traces) to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        if (code.status() >= 500) {
            log.warn("{} on {} {}: {}", code, request.getMethod(), request.getRequestURI(), ex.getMessage());
        }
        return build(code, code.status(), ex.getMessage(), ex.getDetails(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.Detail> details = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.add(new ApiError.Detail(fe.getField(), fe.getDefaultMessage()));
        }
        for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
            details.add(new ApiError.Detail(oe.getObjectName(), oe.getDefaultMessage()));
        }
        return build(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.status(),
                "Request validation failed", details, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST.status(),
                "Malformed or unreadable request body", List.of(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST.status(),
                "Invalid value for parameter '" + ex.getName() + "'", List.of(), request);
    }

    /** Standard Spring MVC failures (unknown path, wrong method, wrong media type, missing parameter). */
    @ExceptionHandler({
            NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleFrameworkException(Exception ex, HttpServletRequest request) {
        int status = 400;
        String message = "Bad request";
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            status = er.getStatusCode().value();
            String detail = er.getBody().getDetail();
            if (detail != null && !detail.isBlank()) {
                message = detail;
            }
        }
        return build(codeFor(status), status, message, List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.status(),
                "An unexpected error occurred", List.of(), request);
    }

    private static ErrorCode codeFor(int status) {
        return switch (status) {
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 422 -> ErrorCode.VALIDATION_FAILED;
            default -> status >= 500 ? ErrorCode.INTERNAL_ERROR : ErrorCode.BAD_REQUEST;
        };
    }

    private static ResponseEntity<ApiError> build(ErrorCode code, int status, String message,
                                                  List<ApiError.Detail> details, HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(), status, code.name(), message, details, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
