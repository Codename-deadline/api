package xyz.om3lette.deadlines_api.data.scopes.userScope.enums

enum class ScopeRole(val assignedToScopeType: ScopeType, val rank: Int) {
    ORG_MEMBER(ScopeType.ORGANIZATION, 0),
    DDL_ASSIGNEE(ScopeType.DEADLINE, 1),
    THR_ASSIGNEE(ScopeType.THREAD, 2),
    THR_ADMIN(ScopeType.THREAD, 3),
    THR_OWNER(ScopeType.THREAD, 4),
    ORG_ADMIN(ScopeType.ORGANIZATION, 5),
    ORG_OWNER(ScopeType.ORGANIZATION, 6),
    ;

    fun canBeAssignedInScope(scope: ScopeType): Boolean = this.assignedToScopeType == scope

    fun isEqualOrHigherThan(role: ScopeRole): Boolean = rank >= role.rank

    fun isHigherThan(role: ScopeRole): Boolean = rank > role.rank

    fun getNextLowerRoleOrLowest() = fromInt(if (rank == 0) 0 else rank - 1)

    companion object {
        fun fromInt(value: Int) = entries.first { it.rank == value }

        fun getLowest() = fromInt(0)
    }
}
