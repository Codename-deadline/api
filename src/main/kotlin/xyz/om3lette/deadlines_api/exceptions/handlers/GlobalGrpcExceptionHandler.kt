package xyz.om3lette.deadlines_api.exceptions.handlers

import io.grpc.Metadata
import io.grpc.StatusException
import io.grpc.Status
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.grpc.server.exception.GrpcExceptionHandler
import org.springframework.stereotype.Component
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.proto.Locale


@Component
class GlobalGrpcExceptionHandler : GrpcExceptionHandler {
    private val localeKey = Metadata.Key.of("locale", Metadata.ASCII_STRING_MARSHALLER)

    override fun handleException(exception: Throwable): StatusException {
        return when (exception) {
            // @Deprecated("Use GrpcKeyLocaleException instead")
            is StatusCodeException -> {
                val metadata = Metadata().apply { put(localeKey, Locale.EN.name) }
                when (exception.statusCode) {
                    400 -> Status.INVALID_ARGUMENT
                    401 -> Status.UNAUTHENTICATED
                    403 -> Status.PERMISSION_DENIED
                    404 -> Status.NOT_FOUND
                    409 -> Status.ALREADY_EXISTS
                    else -> Status.UNKNOWN
                }
                    .withDescription("errors.${exception.detail}")
                    .asException(metadata)
            }
            is GrpcKeyLocaleException -> {
                val metadata = Metadata().apply { put(localeKey, exception.locale.name) }
                exception.status
                    .withDescription(exception.key)
                    .asException(metadata)
            }
            else -> {
                val metadata = Metadata().apply { put(localeKey, Locale.EN.name) }
                Status.INTERNAL
                    .withDescription("errors.server_internal")
                    .withCause(exception)
                    .asException(metadata)
            }
        }
    }
}
