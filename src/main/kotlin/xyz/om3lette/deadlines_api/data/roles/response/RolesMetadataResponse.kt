package xyz.om3lette.deadlines_api.data.roles.response

data class RolesMetadataResponse(
    val roles: List<String>,
    val matrix: List<List<Boolean>>
)
