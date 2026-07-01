package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("storage")
data class StorageProperties(
    val s3: S3
) {
    data class S3(
        val endpoint: String,
        val region: String = "garage",
        val bucket: String = "deadlines-attachments",
        val accessKey: String = "",
        val secretKey: String = "",
        val createBucket: Boolean = true
    )
}
