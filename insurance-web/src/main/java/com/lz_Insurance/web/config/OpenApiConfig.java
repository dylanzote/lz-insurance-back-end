package com.lz_Insurance.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP;

/**
 * OpenAPI configuration for Swagger documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lz-insurance API")
                        .description("API documentation for LZ-insurance")
                        .contact(new Contact()
                                .name("LZ Insurance")
                                .email("support@insurance.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://lz-insurance.com"))
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token for authentication")));
    }
}
