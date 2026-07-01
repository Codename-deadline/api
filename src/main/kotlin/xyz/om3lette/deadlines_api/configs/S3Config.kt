package xyz.om3lette.deadlines_api.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import xyz.om3lette.deadlines_api.configs.properties.StorageProperties
import java.net.URI


@Configuration
@Profile("!test")
class S3Config(
    private val storageProperties: StorageProperties
) {
    private val s3Properties = storageProperties.s3
    private val credentialsProvider = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(s3Properties.accessKey, s3Properties.secretKey)
    )
    private val serviceConfiguration = S3Configuration.builder()
        .pathStyleAccessEnabled(s3Properties.pathStyleAccessEnabled)
        .chunkedEncodingEnabled(false)
        .build()

    @Bean
    fun s3Client(): S3Client = S3Client.builder()
        .endpointOverride(URI.create(s3Properties.endpoint))
        .region(Region.of(s3Properties.region))
        .credentialsProvider(credentialsProvider)
        .serviceConfiguration(serviceConfiguration)
        .httpClientBuilder(ApacheHttpClient.builder())
        .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
        .build()

    @Bean
    fun s3Presigner(): S3Presigner = S3Presigner.builder()
        .endpointOverride(URI.create(s3Properties.publicEndpoint))
        .region(Region.of(s3Properties.region))
        .credentialsProvider(credentialsProvider)
        .serviceConfiguration(serviceConfiguration)
        .build()
}
