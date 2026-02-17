package org.sample.simpleenterprizeproj2.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.sample.simpleenterprizeproj2.model.WebhookDeliveryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, Long> {

    List<WebhookDeliveryLog> findBySuccessFalseAndAttemptCountLessThan(int maxAttempts);

    void deleteByCreatedAtBefore(LocalDateTime cutoff);

    Page<WebhookDeliveryLog> findByWebhookId(Long webhookId, Pageable pageable);
}
