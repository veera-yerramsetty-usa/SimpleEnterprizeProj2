package org.sample.simpleenterprizeproj2.config;

import java.net.URISyntaxException;
import java.util.Map;

import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateCacheConfig implements HibernatePropertiesCustomizer {

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        try {
            hibernateProperties.put(ConfigSettings.CONFIG_URI,
                    getClass().getResource("/ehcache.xml").toURI().toString());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Failed to resolve ehcache.xml URI", e);
        }
    }
}
