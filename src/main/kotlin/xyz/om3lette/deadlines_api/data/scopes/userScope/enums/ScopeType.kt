package xyz.om3lette.deadlines_api.data.scopes.userScope.enums

enum class ScopeType(val code: String) {
    ORGANIZATION("O"),
    THREAD("T"),
    DEADLINE("D");

    companion object {
        fun fromCode(c: String) = entries.first { it.code == c }
    }
}

