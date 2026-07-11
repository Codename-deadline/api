package xyz.om3lette.deadlines_api.services.integration

import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.common.dto.IssuerContext
import xyz.om3lette.deadlines_api.data.integration.common.enums.IntegrationResultKey
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.requirePermissionGrpc

@Service
class IntegrationPermissionValidator(
    private val permissionService: PermissionService
) {
    fun requireChatManagementPermission(
        issuerContext: IssuerContext,
        issuerHasMessengerChatAdminRights: Boolean
    ) {
        requirePermissionGrpc(
            permissionService.canManageIntegrationChat(
                issuerContext.user,
                issuerHasMessengerChatAdminRights
            ),
            IntegrationResultKey.CHAT_MANAGEMENT_DENIED.value(),
            { issuerContext.language }
        )
    }
}
