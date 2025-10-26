package xyz.om3lette.deadlines_api.controllers.deadline

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.scopes.deadline.requests.PatchDeadlineRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.DeadlineService


@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/deadlines/{deadlineId}")
@Tag(name = "Deadlines")
class DeadlineController(
    val deadlineService: DeadlineService
) {
    @DeleteMapping
    fun deleteDeadline(
        @AuthenticationPrincipal user: User,
        @PathVariable deadlineId: Long
    ) = deadlineService.deleteDeadline(user, deadlineId)

    @GetMapping
    fun getDeadline(
        @AuthenticationPrincipal user: User,
        @PathVariable deadlineId: Long
    ) = deadlineService.getDeadlineMetaData(user, deadlineId)

    @PatchMapping
    fun patchDeadline(
        @AuthenticationPrincipal user: User,
        @PathVariable deadlineId: Long,
        @RequestBody request: PatchDeadlineRequest
    ) = deadlineService.patchDeadline(
        user,
        deadlineId,
        request.title,
        request.description,
        request.progress,
        request.status,
        request.due
    )

    @GetMapping("/assignees")
    fun getAssignees(
        @AuthenticationPrincipal user: User,
        @PathVariable deadlineId: Long,
        @RequestParam("page") pageNumber: Int
    ) = deadlineService.getDeadlineAssignees(user, deadlineId, pageNumber, 10)

    @DeleteMapping("/assignees/{assigneeUsername}")
    fun removeAssignee(
        @AuthenticationPrincipal user: User,
        @PathVariable deadlineId: Long,
        @PathVariable assigneeUsername: String
    ) = deadlineService.removeAssignee(user, deadlineId, assigneeUsername)

}
