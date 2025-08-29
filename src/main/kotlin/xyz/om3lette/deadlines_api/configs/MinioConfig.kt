package xyz.om3lette.deadlines_api.configs

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class MinioConfig(
    @param:Value("\${minio.url}")       private val url: String,
    @param:Value("\${minio.accessKey}") private val accessKey: String,
    @param:Value("\${minio.secretKey}") private val secretKey: String
) {

    @Bean
    fun minioClient() = MinioClient.builder()
        .endpoint(url)
        .credentials(accessKey, secretKey)
        .build()
}