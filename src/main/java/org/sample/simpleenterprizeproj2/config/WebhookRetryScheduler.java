package org.sample.simpleenterprizeproj2.config;

import java.util.List;
import java.util.Optional;

import org.sample.simpleenterprizeproj2.model.WebhookDeliveryLog;
import org.sample.simpleenterprizeproj2.model.WebhookRegistration;
import org.sample.simpleenterprizeproj2.repository.WebhookDeliveryLogRepository;
import org.sample.simpleenterprizeproj2.repository.WebhookRegistrationRepository;
import org.sample.simpleenterprizeproj2.util.WebhookSignatureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Component
public class WebhookRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookRetryScheduler.class);
    private static final int MAX_ATTEMPTS = 3;

    private final WebhookDeliveryLogRepository deliveryLogRepository;
    private final WebhookRegistrationRepository webhookRegistrationRepository;
    private final RestClient webhookRestClient;

    public WebhookRetryScheduler(WebhookDeliveryLogRepository deliveryLogRepository,
                                 WebhookRegistrationRepository webhookRegistrationRepository,
                                 RestClient webhookRestClient) {
        this.deliveryLogRepository = deliveryLogRepository;
        this.webhookRegistrationRepository = webhookRegistrationRepository;
        this.webhookRestClient = webhookRestClient;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void retryFailedDeliveries() {
        List<WebhookDeliveryLog> failedLogs = deliveryLogRepository
                .findBySuccessFalseAndAttemptCountLessThan(MAX_ATTEMPTS);

        for (WebhookDeliveryLog deliveryLog : failedLogs) {
            Optional<WebhookRegistration> webhookOpt = webhookRegistrationRepository
                    .findById(deliveryLog.getWebhookId());

            if (webhookOpt.isEmpty() || !webhookOpt.get().isActive()) {
                log.debug("Skipping retry for delivery log id={} — webhook inactive or deleted",
                        deliveryLog.getId());
                continue;
            }

            WebhookRegistration webhook = webhookOpt.get();
            retryDelivery(deliveryLog, webhook);
        }

        if (!failedLogs.isEmpty()) {
            log.info("Processed {} failed webhook deliveries for retry", failedLogs.size());
        }
    }

    private void retryDelivery(WebhookDeliveryLog deliveryLog, WebhookRegistration webhook) {
        try {
            String payload = deliveryLog.getRequestBody();
            String signature = WebhookSignatureUtils.computeSignature(payload, webhook.getSecret());

            String responseBody = webhookRestClient.post()
                    .uri(webhook.getUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Signature", signature)
                    .header("X-Webhook-Event", deliveryLog.getEventType())
                    .header("X-Webhook-Entity-Type", deliveryLog.getEntityType())
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            deliveryLog.setSuccess(true);
            deliveryLog.setResponseStatus(200);
            deliveryLog.setResponseBody(truncate(responseBody, 1024));
            deliveryLog.setAttemptCount(deliveryLog.getAttemptCount() + 1);
            deliveryLogRepository.save(deliveryLog);
            log.info("Retry succeeded for delivery log id={}", deliveryLog.getId());

        } catch (Exception e) {
            deliveryLog.setAttemptCount(deliveryLog.getAttemptCount() + 1);
            deliveryLog.setResponseBody(truncate(e.getMessage(), 1024));
            deliveryLogRepository.save(deliveryLog);
            log.warn("Retry failed for delivery log id={} (attempt {}): {}",
                    deliveryLog.getId(), deliveryLog.getAttemptCount(), e.getMessage());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
