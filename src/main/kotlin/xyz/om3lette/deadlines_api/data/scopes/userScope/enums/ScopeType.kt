package xyz.om3lette.deadlines_api.data.scopes.userScope.enums

import jakarta.persistence.EnumeratedValue

enum class ScopeType(
    @field:EnumeratedValue val code: String
) {
    ORGANIZATION("ORG"),
    THREAD("THR"),
    DEADLINE("DDL");

    companion object {
        fun fromCode(code: String) = entries.first { it.code == code }
    }
}
