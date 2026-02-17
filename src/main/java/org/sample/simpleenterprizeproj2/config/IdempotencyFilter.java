package org.sample.simpleenterprizeproj2.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.sample.simpleenterprizeproj2.model.IdempotencyRecord;
import org.sample.simpleenterprizeproj2.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(10)
public class IdempotencyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyRepository idempotencyRepository;
    private final MessageSource messageSource;

    public IdempotencyFilter(IdempotencyRepository idempotencyRepository, MessageSource messageSource) {
        this.idempotencyRepository = idempotencyRepository;
        this.messageSource = messageSource;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!"POST".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String idempotencyKey = httpRequest.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<IdempotencyRecord> existing = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.getStatusCode() != null) {
                log.info("Replaying idempotent response for key '{}'", idempotencyKey);
                replayResponse(httpResponse, record);
            } else {
                log.warn("Concurrent request detected for idempotency key '{}'", idempotencyKey);
                writeConflictResponse(httpResponse);
            }
            return;
        }

        IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey, httpRequest.getRequestURI(), LocalDateTime.now());
        try {
            idempotencyRepository.save(record);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent request detected for idempotency key '{}'", idempotencyKey);
            writeConflictResponse(httpResponse);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        try {
            chain.doFilter(request, responseWrapper);

            int status = responseWrapper.getStatus();
            if (status >= 500) {
                idempotencyRepository.delete(record);
                log.debug("Deleted idempotency record for key '{}' due to 5xx response", idempotencyKey);
            } else {
                record.setStatusCode(status);
                record.setContentType(responseWrapper.getContentType());
                byte[] body = responseWrapper.getContentAsByteArray();
                if (body.length > 0) {
                    record.setResponseBody(new String(body, responseWrapper.getCharacterEncoding()));
                }
                idempotencyRepository.save(record);
            }
        } catch (Exception e) {
            idempotencyRepository.delete(record);
            log.debug("Deleted idempotency record for key '{}' due to exception", idempotencyKey);
            throw e;
        }

        responseWrapper.copyBodyToResponse();
    }

    private void replayResponse(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getStatusCode());
        if (record.getContentType() != null) {
            response.setContentType(record.getContentType());
        }
        if (record.getResponseBody() != null) {
            response.getWriter().write(record.getResponseBody());
        }
    }

    private void writeConflictResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/json");

        String message = messageSource.getMessage("error.idempotency.conflict", null,
                "A request with this idempotency key is already being processed",
                LocaleContextHolder.getLocale());

        String json = "{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":409,"
                + "\"error\":\"Conflict\","
                + "\"message\":\"" + message + "\"}";

        response.getWriter().write(json);
    }
}
