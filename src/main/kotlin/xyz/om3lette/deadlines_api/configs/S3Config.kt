package xyz.om3lette.deadlines_api.configs

import io.minio.MinioClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import xyz.om3lette.deadlines_api.configs.properties.StorageProperties


@Configuration
@Profile("!test")
class S3Config(
    private val storageProperties: StorageProperties
) {

    @Bean
    fun s3Client() = MinioClient.builder()
        .endpoint(storageProperties.s3.endpoint)
        .credentials(storageProperties.s3.accessKey, storageProperties.s3.secretKey)
        .build()
}
