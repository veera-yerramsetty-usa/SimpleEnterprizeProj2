package org.sample.simpleenterprizeproj2.config;

import java.time.Duration;
import java.time.Instant;

import org.sample.simpleenterprizeproj2.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class IdempotencyCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupScheduler.class);

    private final IdempotencyRepository idempotencyRepository;

    public IdempotencyCleanupScheduler(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredKeys() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        idempotencyRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Cleaned up idempotency keys older than {}", cutoff);
    }
}
