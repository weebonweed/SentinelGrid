package com.sentinelgrid.handler;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sentinelgrid.dto.response.ErrorResponse;
import com.sentinelgrid.exception.CooldownViolationException;
import com.sentinelgrid.exception.DepthLimitExceededException;
import com.sentinelgrid.exception.RateLimitExceededException;
import com.sentinelgrid.exception.ResourceNotFoundException;
import com.sentinelgrid.exception.ServiceException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex,
                                                          HttpServletRequest request) {
        log.warn("Rate limit exceeded: path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request, null);
    }

    @ExceptionHandler(CooldownViolationException.class)
    public ResponseEntity<ErrorResponse> handleCooldown(CooldownViolationException ex,
                                                         HttpServletRequest request) {
        log.warn("Cooldown violation: path={} message={}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request, null);
    }

    @ExceptionHandler(DepthLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDepthLimit(DepthLimitExceededException ex,
                                                           HttpServletRequest request) {
        log.warn("Depth limit exceeded: path={}", request.getRequestURI());
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                         HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                fe -> fe.getField(),
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                (a, b) -> a
            ));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                              HttpServletRequest request) {
        log.warn("Data integrity violation: path={}", request.getRequestURI());
        return buildResponse(HttpStatus.CONFLICT, "Resource already exists or constraint violated", request, null);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleService(ServiceException ex,
                                                        HttpServletRequest request) {
        log.error("Service failure: path={} message={}", request.getRequestURI(), ex.getMessage(), ex.getCause());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception: path={}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message,
                                                          HttpServletRequest request,
                                                          Map<String, String> fieldErrors) {
        ErrorResponse body = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(status.value())
            .error(message)
            .path(request.getRequestURI())
            .correlationId(MDC.get("correlationId"))
            .fieldErrors(fieldErrors)
            .build();
        return ResponseEntity.status(status).body(body);
    }
}
