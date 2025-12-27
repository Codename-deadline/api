package xyz.om3lette.deadlines_api.services.permission

import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import java.util.Optional

@Service
class PermissionLookupService(
    private val deadlineRepository: DeadlineRepository,
    private val threadRepository: ThreadRepository,
    private val userScopeRepository: UserScopeRepository
) {
    private fun findHighestRoleUserScopeWithScopeIdIn(issuer: User, scopeIds: List<Long>): Optional<UserScope> =
        Optional.of(
            userScopeRepository.findByUserAndScopeIdIn(issuer, scopeIds)
                .maxBy { it.role }
        )

    fun getDeadlineAndHighestRoleUserScopeOr404(issuer: User, deadlineId: Long): Pair<Deadline, () -> Optional<UserScope>> {
        val deadline: Deadline = deadlineRepository.findByIdOr404(deadlineId, ErrorCode.DDL_NOT_FOUND)
        return Pair(
            deadline
        ) {
            findHighestRoleUserScopeWithScopeIdIn(
                issuer,
                listOf(deadlineId, deadline.thread.id, deadline.thread.organization.id)
            )
        }
    }

    fun getHighestRoleUserScopeOr404(issuer: User, deadline: Deadline): () -> Optional<UserScope> =
        {
            findHighestRoleUserScopeWithScopeIdIn(
                issuer, listOf(deadline.id, deadline.thread.id, deadline.thread.organization.id)
            )
        }


    fun getThreadAndHighestRoleUserScopeOr404(issuer: User, threadId: Long): Pair<Thread, () -> Optional<UserScope>> {
        val thread: Thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)
        return Pair(
            thread
        ) {
            findHighestRoleUserScopeWithScopeIdIn(
                issuer,
                listOf(threadId, thread.organization.id)
            )
        }
    }
}