package xyz.om3lette.deadlines_api.util.minioClient

import io.minio.GetObjectArgs
import io.minio.GetObjectResponse
import io.minio.MinioClient
import io.minio.ObjectWriteResponse
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs

inline fun MinioClient.putObject(
    bucket: String,
    key: String,
    block: PutObjectArgs.Builder.() -> Unit
): ObjectWriteResponse =
    this.putObject(
        PutObjectArgs.builder()
            .bucket(bucket)
            .`object`(key)
            .apply(block)
            .build()
    )


fun MinioClient.getObject(
    bucket: String,
    key: String,
): GetObjectResponse =
    this.getObject(
        GetObjectArgs.builder()
            .bucket(bucket)
            .`object`(key)
            .build()
    )

fun MinioClient.removeObject(
    bucket: String,
    key: String
) =
    this.removeObject(
        RemoveObjectArgs.builder()
            .bucket(bucket)
            .`object`(key)
            .build()
    )

fun MinioClient.statObject(
    bucket: String,
    key: String
) =
    StatObjectArgs.builder()
        .bucket(bucket)
        .`object`(key)
        .build()

