package xyz.om3lette.deadlines_api.util

import io.grpc.Status
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException

inline fun requirePermission(
    granted: Boolean,
    lazyMessage: () -> String = { "Access denied: insufficient permissions." },
    httpStatus: Int = 403
) {
    if (granted) return
    throw StatusCodeException(httpStatus, lazyMessage())
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