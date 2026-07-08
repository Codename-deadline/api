package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.time.DurationMin
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties("storage")
data class StorageProperties(
    @field:Valid
    val s3: S3
) {
    data class S3(
        @field:NotBlank
        val endpoint: String,

        @field:NotBlank
        val publicEndpoint: String = endpoint,

        @field:NotBlank
        val region: String = "garage",

        @field:NotBlank
        val bucket: String = "deadlines-attachments",
        val accessKey: String = "",
        val secretKey: String = "",
        val pathStyleAccessEnabled: Boolean = true,

        @field:DurationMin(seconds = 1)
        val presignedUrlExpiration: Duration = Duration.ofMinutes(5)
    )
}
