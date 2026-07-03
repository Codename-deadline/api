package xyz.om3lette.deadlines_api.config

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@TestConfiguration
class TestInfraMocks {
    @Bean
    fun s3Client(): S3Client =
        Mockito.mock(S3Client::class.java)

    @Bean
    fun s3Presigner(): S3Presigner =
        Mockito.mock(S3Presigner::class.java)
}
