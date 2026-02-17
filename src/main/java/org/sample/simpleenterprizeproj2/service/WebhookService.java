package org.sample.simpleenterprizeproj2.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.sample.simpleenterprizeproj2.dto.WebhookDeliveryLogResponse;
import org.sample.simpleenterprizeproj2.dto.WebhookRegistrationPatchRequest;
import org.sample.simpleenterprizeproj2.dto.WebhookRegistrationRequest;
import org.sample.simpleenterprizeproj2.dto.WebhookRegistrationResponse;
import org.sample.simpleenterprizeproj2.exception.ResourceNotFoundException;
import org.sample.simpleenterprizeproj2.exception.ServiceUnavailableException;
import org.sample.simpleenterprizeproj2.mapper.WebhookRegistrationMapper;
import org.sample.simpleenterprizeproj2.model.WebhookRegistration;
import org.sample.simpleenterprizeproj2.repository.WebhookDeliveryLogRepository;
import org.sample.simpleenterprizeproj2.repository.WebhookRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookRegistrationRepository webhookRegistrationRepository;
    private final WebhookDeliveryLogRepository webhookDeliveryLogRepository;
    private final WebhookRegistrationMapper webhookMapper;

    public WebhookService(WebhookRegistrationRepository webhookRegistrationRepository,
                          WebhookDeliveryLogRepository webhookDeliveryLogRepository,
                          WebhookRegistrationMapper webhookMapper) {
        this.webhookRegistrationRepository = webhookRegistrationRepository;
        this.webhookDeliveryLogRepository = webhookDeliveryLogRepository;
        this.webhookMapper = webhookMapper;
    }

    @CircuitBreaker(name = "webhookService", fallbackMethod = "findAllFallback")
    @Bulkhead(name = "webhookService")
    @Retry(name = "webhookService")
    public Page<WebhookRegistrationResponse> findAll(Pageable pageable) {
        return webhookRegistrationRepository.findAll(pageable)
                .map(webhookMapper::toResponse);
    }

    @CircuitBreaker(name = "webhookService", fallbackMethod = "findByIdFallback")
    @Bulkhead(name = "webhookService")
    @Retry(name = "webhookService")
    public WebhookRegistrationResponse findById(Long id) {
        return webhookMapper.toResponse(findEntityById(id));
    }

    @CircuitBreaker(name = "webhookService", fallbackMethod = "findEntityByIdFallback")
    @Bulkhead(name = "webhookService")
    @Retry(name = "webhookService")
    public WebhookRegistration findEntityById(Long id) {
        return webhookRegistrationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook not found with id " + id));
    }

    @CircuitBreaker(name = "webhookService", fallbackMethod = "createFallback")
    @Bulkhead(name = "webhookService")
    @Retry(name = "webhookService")
    @Transactional
    public WebhookRegistrationResponse create(WebhookRegistrationRequest request) {
        WebhookRegistration entity = webhookMapper.toEntity(request);
        entity.setCreatedAt(LocalDateTime.now());
        WebhookRegistrationResponse response = webhookMapper.toResponse(
                webhookRegistrationRepository.save(entity));
        log.info("Created webhook id={}", response.getId());
        return response;
    }

    @CircuitBreaker(name = "webhookService", fallbackMethod = "updateFallback")
    @Bulkhead(name = "webhookService")
    @Retry(name = "webhookService")
    @Transactional
    public WebhookRegistrationResponse update(Long id, WebhookRegistrationRequest request) {
        WebhookRegistration entity = findEntityById(id);
        webhookMapper.updateEntity(entity, request);
        WebhookRegistrationResponse response = webhookMapper.toResponse(
                webhookRegistrationRepository.save(entity));
        log.info("Updated webhook id={}", id);
        return response;
    }

    @CircuitBreaker(name = "webhookService", fallbackMethod = "patchFallback")
    @Bulkhead(name = "webhookService")
    @Retry(name = "webhookService")
    @Transactional
    public WebhookRegistrationResponse patch(Long id, WebhookRegistrationPatchRequest request) {
        WebhookRegistration entity = findEntityById(id);
        webhookMapper.patchEntity(entity, request);
        WebhookRegistrationResponse response = webhookMapper.toResponse(
                webhookRegistrationRepository.save(entity));
        log.info("Patched webhook id={}", id);
        return response;
    }

    @CircuitBreaker(name = "webhookService", fallbackMethod = "deleteFallback")
    @Bulkhead(name = "webhookService")
    @Retry(name = "webhookService")
    @Transactional
    public void delete(Long id) {
        WebhookRegistration entity = findEntityById(id);
        webhookRegistrationRepository.delete(entity);
        log.info("Deleted webhook id={}", id);
    }

    @CircuitBreaker(name = "webhookService", fallbackMethod = "findDeliveryLogsFallback")
    @Bulkhead(name = "webhookService")
    @Retry(name = "webhookService")
    public Page<WebhookDeliveryLogResponse> findDeliveryLogsByWebhookId(Long webhookId, Pageable pageable) {
        findEntityById(webhookId);
        return webhookDeliveryLogRepository.findByWebhookId(webhookId, pageable)
                .map(webhookMapper::toDeliveryLogResponse);
    }

    // ── Fallback methods ──────────────────────────────────────────────

    private Page<WebhookRegistrationResponse> findAllFallback(Pageable pageable, Throwable t) {
        log.warn("Fallback for findAll triggered: {}", t.getMessage());
        return Page.empty(pageable);
    }

    private WebhookRegistrationResponse findByIdFallback(Long id, Throwable t) {
        log.warn("Fallback for findById(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Webhook service is temporarily unavailable", t);
    }

    private WebhookRegistration findEntityByIdFallback(Long id, Throwable t) {
        log.warn("Fallback for findEntityById(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Webhook service is temporarily unavailable", t);
    }

    private WebhookRegistrationResponse createFallback(WebhookRegistrationRequest request, Throwable t) {
        log.warn("Fallback for create triggered: {}", t.getMessage());
        throw new ServiceUnavailableException("Webhook service is temporarily unavailable", t);
    }

    private WebhookRegistrationResponse updateFallback(Long id, WebhookRegistrationRequest request, Throwable t) {
        log.warn("Fallback for update(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Webhook service is temporarily unavailable", t);
    }

    private WebhookRegistrationResponse patchFallback(Long id, WebhookRegistrationPatchRequest request, Throwable t) {
        log.warn("Fallback for patch(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Webhook service is temporarily unavailable", t);
    }

    private void deleteFallback(Long id, Throwable t) {
        log.warn("Fallback for delete(id={}) triggered: {}", id, t.getMessage());
        throw new ServiceUnavailableException("Webhook service is temporarily unavailable", t);
    }

    private Page<WebhookDeliveryLogResponse> findDeliveryLogsFallback(Long webhookId, Pageable pageable, Throwable t) {
        log.warn("Fallback for findDeliveryLogs(webhookId={}) triggered: {}", webhookId, t.getMessage());
        return Page.empty(pageable);
    }
}
