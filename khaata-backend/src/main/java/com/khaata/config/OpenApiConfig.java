package com.khaata.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger/OpenAPI metadata, available at /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI khaataOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Khaata API")
                .version("1.0.0")
                .description("Digital udhaar-book: customers, append-only ledger, and reminders."));
    }
}
