package org.sample.simpleenterprizeproj2.dto;

import java.time.Instant;

public class WebhookRegistrationResponse {

    private Long id;
    private String url;
    private String entityType;
    private String eventType;
    private boolean active;
    private Instant createdAt;

    public WebhookRegistrationResponse() {}

    public WebhookRegistrationResponse(Long id, String url, String entityType, String eventType,
                                       boolean active, Instant createdAt) {
        this.id = id;
        this.url = url;
        this.entityType = entityType;
        this.eventType = eventType;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
