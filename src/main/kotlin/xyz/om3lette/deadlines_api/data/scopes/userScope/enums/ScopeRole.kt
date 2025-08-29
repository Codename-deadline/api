package xyz.om3lette.deadlines_api.data.scopes.userScope.enums

enum class ScopeRole {
    ORG_MEMBER,
    DDL_ASSIGNEE,
    THR_ASSIGNEE,
    ORG_ADMIN,
    ORG_OWNER,
    ;

    fun isEqualOrHigherThan(role: ScopeRole): Boolean = this.ordinal >= role.ordinal

    fun isHigherThan(role: ScopeRole): Boolean = this.ordinal > role.ordinal

    fun getNextLowerRoleOrLowest() = fromInt(if (ordinal == 0) 0 else ordinal - 1)

    companion object {
        fun fromInt(value: Int) = entries.first { it.ordinal == value }

        fun getLowest() = fromInt(0)
    }
}
