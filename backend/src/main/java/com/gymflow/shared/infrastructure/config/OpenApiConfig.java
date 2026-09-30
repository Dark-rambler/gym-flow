package com.gymflow.shared.infrastructure.config;

import java.util.ArrayList;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * - Los DTO *Response siempre traen todos sus campos: se marcan como requeridos para que el cliente TS
     *   generado no los tipe como opcionales. (Los *Request ya salen requeridos por @NotNull/@NotBlank.)
     * - Esquema Bearer JWT global para probar desde Swagger UI.
     */
    @Bean
    OpenApiCustomizer gymflowOpenApiCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components.getSchemas() != null) {
                components.getSchemas().forEach((name, schema) -> {
                    if (name.endsWith("Response") && schema.getProperties() != null) {
                        schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
                    }
                });
            }
            components.addSecuritySchemes("bearer",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
            openApi.addSecurityItem(new SecurityRequirement().addList("bearer"));
        };
    }
}
