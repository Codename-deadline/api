package xyz.om3lette.deadlines_api.data.scopes.userScope.enums

enum class ScopeRole(val assignedToScopeType: ScopeType) {
    ORG_MEMBER(ScopeType.ORGANIZATION),
    DDL_ASSIGNEE(ScopeType.DEADLINE),
    THR_ASSIGNEE(ScopeType.THREAD),
    THR_ADMIN(ScopeType.THREAD),
    THR_OWNER(ScopeType.THREAD),
    ORG_ADMIN(ScopeType.ORGANIZATION),
    ORG_OWNER(ScopeType.ORGANIZATION),
    ;

    fun canBeAssignedInScope(scope: ScopeType): Boolean = this.assignedToScopeType == scope

    fun isEqualOrHigherThan(role: ScopeRole): Boolean = this.ordinal >= role.ordinal

    fun isHigherThan(role: ScopeRole): Boolean = this.ordinal > role.ordinal

    fun getNextLowerRoleOrLowest() = fromInt(if (ordinal == 0) 0 else ordinal - 1)

    companion object {
        fun fromInt(value: Int) = entries.first { it.ordinal == value }

        fun getLowest() = fromInt(0)
    }
}
