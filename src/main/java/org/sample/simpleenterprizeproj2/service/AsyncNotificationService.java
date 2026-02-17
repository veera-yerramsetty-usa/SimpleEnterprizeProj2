package org.sample.simpleenterprizeproj2.service;

import java.time.LocalDateTime;
import java.util.List;

import org.sample.simpleenterprizeproj2.model.WebhookDeliveryLog;
import org.sample.simpleenterprizeproj2.model.WebhookRegistration;
import org.sample.simpleenterprizeproj2.repository.WebhookDeliveryLogRepository;
import org.sample.simpleenterprizeproj2.repository.WebhookRegistrationRepository;
import org.sample.simpleenterprizeproj2.util.WebhookSignatureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AsyncNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationService.class);

    private final WebhookRegistrationRepository webhookRegistrationRepository;
    private final WebhookDeliveryLogRepository webhookDeliveryLogRepository;
    private final RestClient webhookRestClient;

    public AsyncNotificationService(WebhookRegistrationRepository webhookRegistrationRepository,
                                    WebhookDeliveryLogRepository webhookDeliveryLogRepository,
                                    RestClient webhookRestClient) {
        this.webhookRegistrationRepository = webhookRegistrationRepository;
        this.webhookDeliveryLogRepository = webhookDeliveryLogRepository;
        this.webhookRestClient = webhookRestClient;
    }

    @Async
    public void notifyResourceCreated(String entityType, Long id) {
        log.info("Dispatching creation notification for {} id={}", entityType, id);
        dispatchWebhooks(entityType, "CREATED", id);
    }

    @Async
    public void notifyResourceUpdated(String entityType, Long id) {
        log.info("Dispatching update notification for {} id={}", entityType, id);
        dispatchWebhooks(entityType, "UPDATED", id);
    }

    @Async
    public void notifyResourceDeleted(String entityType, Long id) {
        log.info("Dispatching deletion notification for {} id={}", entityType, id);
        dispatchWebhooks(entityType, "DELETED", id);
    }

    private void dispatchWebhooks(String entityType, String eventType, Long entityId) {
        List<WebhookRegistration> webhooks = webhookRegistrationRepository
                .findActiveByEntityTypeAndEventType(entityType, eventType);

        if (webhooks.isEmpty()) {
            return;
        }

        String payload = buildPayload(entityType, eventType, entityId);

        for (WebhookRegistration webhook : webhooks) {
            deliverWebhook(webhook, entityType, eventType, entityId, payload);
        }
    }

    private String buildPayload(String entityType, String eventType, Long entityId) {
        return "{\"entityType\":\"" + entityType
                + "\",\"eventType\":\"" + eventType
                + "\",\"entityId\":" + entityId
                + ",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
    }

    private void deliverWebhook(WebhookRegistration webhook, String entityType,
                                String eventType, Long entityId, String payload) {
        WebhookDeliveryLog deliveryLog = new WebhookDeliveryLog();
        deliveryLog.setWebhookId(webhook.getId());
        deliveryLog.setEntityType(entityType);
        deliveryLog.setEventType(eventType);
        deliveryLog.setEntityId(entityId);
        deliveryLog.setRequestUrl(webhook.getUrl());
        deliveryLog.setRequestBody(payload);
        deliveryLog.setCreatedAt(LocalDateTime.now());
        deliveryLog.setAttemptCount(1);

        try {
            String signature = WebhookSignatureUtils.computeSignature(payload, webhook.getSecret());

            String responseBody = webhookRestClient.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Signature", signature)
                    .header("X-Webhook-Event", eventType)
                    .header("X-Webhook-Entity-Type", entityType)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            deliveryLog.setSuccess(true);
            deliveryLog.setResponseStatus(200);
            deliveryLog.setResponseBody(truncate(responseBody, 1024));
            log.info("Webhook delivered successfully to {} for {} {} id={}",
                    webhook.getUrl(), entityType, eventType, entityId);

        } catch (Exception e) {
            deliveryLog.setSuccess(false);
            deliveryLog.setResponseBody(truncate(e.getMessage(), 1024));
            log.warn("Webhook delivery failed to {} for {} {} id={}: {}",
                    webhook.getUrl(), entityType, eventType, entityId, e.getMessage());
        }

        webhookDeliveryLogRepository.save(deliveryLog);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
