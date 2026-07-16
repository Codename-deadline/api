package xyz.om3lette.deadlines_api.configs

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import xyz.om3lette.deadlines_api.data.common.validation.ValidationErrorResponse

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI {
        val bearerSchema = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .name("Auth")

        val authTag = Tag().name("Authentication")

        return OpenAPI()
            .components(Components().addSecuritySchemes("bearerAuth", bearerSchema))
            .tags(listOf(authTag))
    }

    @Bean
    fun validationErrorResponseCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        val schemas = ModelConverters.getInstance().readAll(ValidationErrorResponse::class.java)
        openApi.components.schemas.putAll(schemas)
        openApi.paths?.values?.flatMap { it.readOperations() }?.forEach { operation ->
            operation.responses.putIfAbsent(
                "422",
                ApiResponse()
                    .description("Request validation failed")
                    .content(
                        Content().addMediaType(
                            org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                            MediaType().schema(
                                Schema<ValidationErrorResponse>()
                                    .`$ref`("#/components/schemas/ValidationErrorResponse")
                            )
                        )
                    )
            )
        }
    }
}
