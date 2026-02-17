package org.sample.simpleenterprizeproj2.mapper;

import org.sample.simpleenterprizeproj2.dto.WebhookDeliveryLogResponse;
import org.sample.simpleenterprizeproj2.dto.WebhookRegistrationPatchRequest;
import org.sample.simpleenterprizeproj2.dto.WebhookRegistrationRequest;
import org.sample.simpleenterprizeproj2.dto.WebhookRegistrationResponse;
import org.sample.simpleenterprizeproj2.model.WebhookDeliveryLog;
import org.sample.simpleenterprizeproj2.model.WebhookRegistration;
import org.sample.simpleenterprizeproj2.util.SanitizationUtils;
import org.springframework.stereotype.Component;

@Component
public class WebhookRegistrationMapper {

    public WebhookRegistration toEntity(WebhookRegistrationRequest request) {
        WebhookRegistration entity = new WebhookRegistration();
        entity.setUrl(SanitizationUtils.sanitize(request.getUrl()));
        entity.setEntityType(request.getEntityType());
        entity.setEventType(request.getEventType());
        entity.setSecret(request.getSecret());
        entity.setActive(true);
        return entity;
    }

    public WebhookRegistrationResponse toResponse(WebhookRegistration entity) {
        return new WebhookRegistrationResponse(
                entity.getId(),
                entity.getUrl(),
                entity.getEntityType(),
                entity.getEventType(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    public void updateEntity(WebhookRegistration entity, WebhookRegistrationRequest request) {
        entity.setUrl(SanitizationUtils.sanitize(request.getUrl()));
        entity.setEntityType(request.getEntityType());
        entity.setEventType(request.getEventType());
        entity.setSecret(request.getSecret());
    }

    public void patchEntity(WebhookRegistration entity, WebhookRegistrationPatchRequest request) {
        if (request.getUrl() != null) {
            entity.setUrl(SanitizationUtils.sanitize(request.getUrl()));
        }
        if (request.getEntityType() != null) {
            entity.setEntityType(request.getEntityType());
        }
        if (request.getEventType() != null) {
            entity.setEventType(request.getEventType());
        }
        if (request.getSecret() != null) {
            entity.setSecret(request.getSecret());
        }
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
    }

    public WebhookDeliveryLogResponse toDeliveryLogResponse(WebhookDeliveryLog log) {
        return new WebhookDeliveryLogResponse(
                log.getId(),
                log.getWebhookId(),
                log.getEntityType(),
                log.getEventType(),
                log.getEntityId(),
                log.getRequestUrl(),
                log.getResponseStatus(),
                log.getResponseBody(),
                log.isSuccess(),
                log.getAttemptCount(),
                log.getCreatedAt()
        );
    }
}
