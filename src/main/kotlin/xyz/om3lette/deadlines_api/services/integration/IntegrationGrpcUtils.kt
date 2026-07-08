package xyz.om3lette.deadlines_api.services.integration

import io.grpc.Status
import io.grpc.stub.StreamObserver
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.data.integration.common.response.IntegrationResult
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.exceptions.type.GrpcKeyLocaleException
import xyz.om3lette.deadlines_api.proto.GeneralResponse

fun grpcException(
    status: Status,
    key: IntegrationResultKey,
    language: Language,
    scopeType: ScopeType? = null,
) = GrpcKeyLocaleException(status, key.value(scopeType), language)

fun integrationResult(
    key: IntegrationResultKey,
    language: Language,
    scopeType: ScopeType? = null,
) = IntegrationResult(key.value(scopeType), language)

fun StreamObserver<GeneralResponse>.sendResult(result: IntegrationResult) {
    onNext(result.toResponse())
    onCompleted()
}
