package com.example.VelocityBoard.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configura OpenAPI / Swagger con el esquema de autenticación JWT Bearer.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "VelocityBoard API",
                version = "1.0",
                description = "API para gestión de tareas y usuarios de VelocityBoard"
        )
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Ingresa el token JWT obtenido en /auth/login"
)
public class OpenApiConfig {
}
