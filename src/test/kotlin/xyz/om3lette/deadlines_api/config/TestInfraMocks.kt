package xyz.om3lette.deadlines_api.config

import io.minio.MinioClient
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.KafkaTemplate

@TestConfiguration
class TestInfraMocks {
    @Bean
    fun minioClient(): MinioClient =
        Mockito.mock(MinioClient::class.java)
}