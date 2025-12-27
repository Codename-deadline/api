package xyz.om3lette.deadlines_api.util

import io.grpc.Status
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException

inline fun requirePermission(
    granted: Boolean,
    lazyMessage: () -> Pair<ErrorCode, String?> = { ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS to "Insufficient permissions" },
    httpStatus: Int = 403,
    params: Map<String, Any> = emptyMap()
) {
    if (granted) return
    val (errorCode, message) = lazyMessage();
    throw StatusCodeException(statusCode = httpStatus, code = errorCode, detail = message, params = params)
}

fun requirePermissionGrpc(
    granted: Boolean,
    key: String,
    languageLazy: () -> Language,
    status: Status = Status.PERMISSION_DENIED
) {
    if (granted) return
    throw GrpcKeyLocaleException(status, key, languageLazy())
}