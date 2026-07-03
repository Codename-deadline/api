package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("storage")
data class StorageProperties(
    val s3: S3
) {
    data class S3(
        val endpoint: String,
        val publicEndpoint: String = endpoint,
        val region: String = "garage",
        val bucket: String = "deadlines-attachments",
        val accessKey: String = "",
        val secretKey: String = "",
        val pathStyleAccessEnabled: Boolean = true,
        val presignedUrlExpiration: Duration = Duration.ofMinutes(5)
    )
}
