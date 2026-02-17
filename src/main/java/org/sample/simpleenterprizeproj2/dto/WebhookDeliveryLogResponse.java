package org.sample.simpleenterprizeproj2.dto;

import java.time.LocalDateTime;

public class WebhookDeliveryLogResponse {

    private Long id;
    private Long webhookId;
    private String entityType;
    private String eventType;
    private Long entityId;
    private String requestUrl;
    private Integer responseStatus;
    private String responseBody;
    private boolean success;
    private int attemptCount;
    private LocalDateTime createdAt;

    public WebhookDeliveryLogResponse() {}

    public WebhookDeliveryLogResponse(Long id, Long webhookId, String entityType, String eventType,
                                      Long entityId, String requestUrl, Integer responseStatus,
                                      String responseBody, boolean success, int attemptCount,
                                      LocalDateTime createdAt) {
        this.id = id;
        this.webhookId = webhookId;
        this.entityType = entityType;
        this.eventType = eventType;
        this.entityId = entityId;
        this.requestUrl = requestUrl;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.success = success;
        this.attemptCount = attemptCount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWebhookId() { return webhookId; }
    public void setWebhookId(Long webhookId) { this.webhookId = webhookId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }

    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
