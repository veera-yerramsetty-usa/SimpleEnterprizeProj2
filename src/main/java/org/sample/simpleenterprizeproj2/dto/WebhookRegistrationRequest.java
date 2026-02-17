package org.sample.simpleenterprizeproj2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class WebhookRegistrationRequest {

    @NotBlank(message = "{validation.webhook.url.required}")
    @Size(max = 2048, message = "{validation.webhook.url.size}")
    @Pattern(regexp = "^https?://.*", message = "{validation.webhook.url.pattern}")
    private String url;

    @NotBlank(message = "{validation.webhook.entityType.required}")
    @Pattern(regexp = "^(User|Employee|Department|\\*)$", message = "{validation.webhook.entityType.pattern}")
    private String entityType;

    @NotBlank(message = "{validation.webhook.eventType.required}")
    @Pattern(regexp = "^(CREATED|UPDATED|DELETED|\\*)$", message = "{validation.webhook.eventType.pattern}")
    private String eventType;

    @NotBlank(message = "{validation.webhook.secret.required}")
    @Size(min = 16, max = 255, message = "{validation.webhook.secret.size}")
    private String secret;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}
