package xyz.om3lette.deadlines_api.data.scopes.userScope.enums

enum class ScopeType(val code: String) {
    ORGANIZATION("ORG"),
    THREAD("THR"),
    DEADLINE("DDL");

    companion object {
        fun fromCode(c: String) = entries.first { it.code == c }
    }
}

