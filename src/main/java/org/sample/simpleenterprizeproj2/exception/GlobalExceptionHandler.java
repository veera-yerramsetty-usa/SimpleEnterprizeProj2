package org.sample.simpleenterprizeproj2.exception;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();
        String message;
        if (ex.getMessageCode() != null) {
            message = messageSource.getMessage(ex.getMessageCode(), ex.getMessageArgs(), ex.getMessage(), locale);
        } else {
            message = ex.getMessage();
        }
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage()));
        String message = messageSource.getMessage("error.validation.failed", null, "Input validation failed", locale);
        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Validation Failed", message);
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.put(field, violation.getMessage());
        });
        String message = messageSource.getMessage("error.validation.failed", null, "Input validation failed", locale);
        Map<String, Object> body = buildBody(HttpStatus.BAD_REQUEST, "Validation Failed", message);
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.conflict.unique", null,
                "A resource with the given unique field(s) already exists", locale);
        return buildResponse(HttpStatus.CONFLICT, "Conflict", message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: {}", ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();
        String typeName = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = messageSource.getMessage("error.type.mismatch",
                new Object[]{ex.getName(), typeName},
                String.format("Parameter '%s' must be of type %s", ex.getName(), typeName), locale);
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.malformed.json", null, "Malformed JSON request body", locale);
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", message);
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServiceUnavailableException ex) {
        log.warn("Service unavailable: {}", ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.service.unavailable", null,
                "Service is temporarily unavailable, please try again later", locale);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", message);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreakerOpen(CallNotPermittedException ex) {
        log.warn("Circuit breaker open: {}", ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.service.unavailable", null,
                "Service is temporarily unavailable, please try again later", locale);
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", message);
    }

    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<Map<String, Object>> handleBulkheadFull(BulkheadFullException ex) {
        log.warn("Bulkhead full: {}", ex.getMessage());
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.bulkhead.full", null,
                "Too many concurrent requests, please try again later", locale);
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("error.unexpected", null,
                "An unexpected error occurred", locale);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", message);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(buildBody(status, error, message));
    }

    private Map<String, Object> buildBody(HttpStatus status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return body;
    }
}
