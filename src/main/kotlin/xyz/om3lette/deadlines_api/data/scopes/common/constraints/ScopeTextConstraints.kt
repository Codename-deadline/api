package xyz.om3lette.deadlines_api.data.scopes.common.constraints

object ScopeTextConstraints {
    const val TITLE_MIN = 2
    const val TITLE_MAX = 128
    const val TITLE_PATCH_REGEX = ".*\\S.*"
    const val DESCRIPTION_MAX = 4096
}
