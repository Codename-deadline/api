package xyz.om3lette.deadlines_api.configs

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InitConfig {
    @Bean
    fun createBuckets(
        @Value("\${minio.bucketNames}") bucketNames: List<String>,
        minioClient: MinioClient
    ) : ApplicationRunner = ApplicationRunner {
        for (bucketName in bucketNames) {
            if (
                minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())
            ) continue
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucketName).build()
            )
        }
    }

}