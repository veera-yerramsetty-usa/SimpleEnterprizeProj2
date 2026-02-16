package org.sample.simpleenterprizeproj2.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDataSourceInfo(ApplicationReadyEvent event) {
        HikariDataSource ds = event.getApplicationContext().getBean(HikariDataSource.class);
        logger.info("HikariCP pool '{}' active — max size: {}, min idle: {}",
                ds.getPoolName(), ds.getMaximumPoolSize(), ds.getMinimumIdle());
    }
}
