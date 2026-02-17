package org.sample.simpleenterprizeproj2.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class WebhookRegistrationPatchRequest {

    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
    private String url;

    @Pattern(regexp = "^(User|Employee|Department|\\*)$", message = "Entity type must be User, Employee, Department, or *")
    private String entityType;

    @Pattern(regexp = "^(CREATED|UPDATED|DELETED|\\*)$", message = "Event type must be CREATED, UPDATED, DELETED, or *")
    private String eventType;

    @Size(min = 16, max = 255, message = "Secret must be between 16 and 255 characters")
    private String secret;

    private Boolean active;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
