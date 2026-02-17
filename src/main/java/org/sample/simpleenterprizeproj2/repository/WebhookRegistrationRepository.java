package org.sample.simpleenterprizeproj2.repository;

import java.util.List;

import org.sample.simpleenterprizeproj2.model.WebhookRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookRegistrationRepository extends JpaRepository<WebhookRegistration, Long> {

    @Query("SELECT w FROM WebhookRegistration w WHERE w.active = true " +
           "AND (w.entityType = :entityType OR w.entityType = '*') " +
           "AND (w.eventType = :eventType OR w.eventType = '*')")
    List<WebhookRegistration> findActiveByEntityTypeAndEventType(
            @Param("entityType") String entityType,
            @Param("eventType") String eventType);
}
