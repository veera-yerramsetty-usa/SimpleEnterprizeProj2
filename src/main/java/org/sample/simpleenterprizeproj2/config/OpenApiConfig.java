package org.sample.simpleenterprizeproj2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        Schema<?> errorSchema = new MapSchema()
                .addProperty("timestamp", new StringSchema().example("2026-02-16T10:30:00.000"))
                .addProperty("status", new Schema<Integer>().type("integer").example(400))
                .addProperty("error", new StringSchema().example("Bad Request"))
                .addProperty("message", new StringSchema().example("Input validation failed"));

        return new OpenAPI()
                .info(new Info()
                        .title("SimpleEnterprizeProj2 API")
                        .version("1.0")
                        .description("REST API for managing users, employees, and departments"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSchemas("ErrorResponse", errorSchema));
    }
}
