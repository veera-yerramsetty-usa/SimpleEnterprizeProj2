package org.sample.simpleenterprizeproj2.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AsyncNotificationService.class);

    @Async
    public void notifyResourceCreated(String entityType, Long id) {
        log.info("Dispatching creation notification for {} id={}", entityType, id);
    }

    @Async
    public void notifyResourceUpdated(String entityType, Long id) {
        log.info("Dispatching update notification for {} id={}", entityType, id);
    }

    @Async
    public void notifyResourceDeleted(String entityType, Long id) {
        log.info("Dispatching deletion notification for {} id={}", entityType, id);
    }
}
