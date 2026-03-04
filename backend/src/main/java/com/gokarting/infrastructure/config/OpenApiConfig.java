package com.gokarting.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(@Value("${app.cors.allowed-origins}") String frontendOrigin) {
        return new OpenAPI()
                .info(new Info()
                        .title("Go Karting Booking API")
                        .version("v1")
                        .description("""
                                Production-grade booking platform built with:
                                **Hexagonal Architecture** · **Kafka + Outbox Pattern** · **JWT + Refresh Token Rotation**
                                · **Redis Rate Limiting** · **Flyway Migrations** · **Testcontainers** · **Prometheus Metrics**
                                """)
                        .contact(new Contact().name("Krish").url("https://github.com/krish"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url(frontendOrigin).description("Frontend Origin")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token obtained from POST /api/v1/auth/login")));
    }
}
