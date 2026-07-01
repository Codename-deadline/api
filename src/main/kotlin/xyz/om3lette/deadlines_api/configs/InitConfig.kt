package xyz.om3lette.deadlines_api.configs

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import xyz.om3lette.deadlines_api.configs.properties.StorageProperties

@Configuration
class InitConfig {
    @Bean
    fun createBuckets(
        storageProperties: StorageProperties,
        minioClient: MinioClient
    ) : ApplicationRunner = ApplicationRunner {
        val s3 = storageProperties.s3
        if (s3.createBucket && !minioClient.bucketExists(BucketExistsArgs.builder().bucket(s3.bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(s3.bucket).build())
        }
    }

}
