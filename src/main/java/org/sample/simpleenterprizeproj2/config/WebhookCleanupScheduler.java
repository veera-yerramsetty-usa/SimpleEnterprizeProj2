package org.sample.simpleenterprizeproj2.config;

import java.time.Duration;
import java.time.Instant;

import org.sample.simpleenterprizeproj2.repository.WebhookDeliveryLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class WebhookCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(WebhookCleanupScheduler.class);

    private final WebhookDeliveryLogRepository deliveryLogRepository;

    public WebhookCleanupScheduler(WebhookDeliveryLogRepository deliveryLogRepository) {
        this.deliveryLogRepository = deliveryLogRepository;
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupOldDeliveryLogs() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(7));
        deliveryLogRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleaned up webhook delivery logs older than {}", cutoff);
    }
}
