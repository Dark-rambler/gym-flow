package com.gymflow.shared.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
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
                        // los campos anotados @Schema(nullable = true) quedan opcionales
                        List<String> required = new ArrayList<>();
                        Map<String, Schema<?>> props = schema.getProperties();
                        props.forEach((prop, propSchema) -> {
                            if (!isNullable(propSchema)) {
                                required.add(prop);
                            }
                        });
                        schema.setRequired(required);
                    }
                });
            }
            components.addSecuritySchemes("bearer",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
            openApi.addSecurityItem(new SecurityRequirement().addList("bearer"));
        };
    }

    // OpenAPI 3.0 usa nullable: true; 3.1 lo expresa como type: [x, "null"] (una referencia $ref puede ir en allOf/oneOf)
    private static boolean isNullable(Schema<?> s) {
        if (Boolean.TRUE.equals(s.getNullable())) return true;
        if (s.getTypes() != null && s.getTypes().contains("null")) return true;
        if (s.getOneOf() != null && s.getOneOf().stream().anyMatch(o -> isNullable(o))) return true;
        return s.getAnyOf() != null && s.getAnyOf().stream().anyMatch(o -> isNullable(o));
    }
}
