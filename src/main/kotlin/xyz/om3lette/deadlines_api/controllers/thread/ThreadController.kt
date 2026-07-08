package xyz.om3lette.deadlines_api.controllers.thread

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.common.constraints.PaginationConstraints
import xyz.om3lette.deadlines_api.data.scopes.thread.request.AddThreadAssigneeRequest
import xyz.om3lette.deadlines_api.data.scopes.thread.request.PatchThreadRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.ThreadService

@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/threads/{threadId}")
@Tag(name = "Threads")
class ThreadController(
    val threadService: ThreadService
) {
    @DeleteMapping
    @Operation(summary = "Delete thread")
    fun deleteThread(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive threadId: Long
    ) = threadService.deleteThread(user, threadId)

    @GetMapping
    @Operation(summary = "Get thread data")
    fun getThread(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive threadId: Long
    ) = threadService.getThread(user, threadId)

    @PatchMapping
    @Operation(summary = "Update thread")
    fun patchThread(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive threadId: Long,
        @Valid @RequestBody request: PatchThreadRequest
    ) = threadService.patchThread(
        user,
        threadId,
        request.title,
        request.description
    )

    @GetMapping("/assignees")
    @Operation(
        summary = "Get thread assignees",
        description = "Returns a list of explicit thread assignees. Higher role organization members are not included."
    )
    fun getAssignees(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive threadId: Long,
        @RequestParam("page") @Min(PaginationConstraints.PAGE_MIN) pageNumber: Int
    ) = threadService.getThreadAssignees(user, threadId, pageNumber, 10)

    @PostMapping("/assignees")
    @Operation(summary = "Add thread assignee")
    fun addAssignees(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive threadId: Long,
        @Valid @RequestBody request: AddThreadAssigneeRequest
    ) = threadService.addAssignee(user, threadId, request.username, request.role)

    @DeleteMapping("/assignees/{assigneeUsername}")
    @Operation(summary = "Remove assignee")
    fun removeAssignee(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive threadId: Long,
        @PathVariable assigneeUsername: String
    ) = threadService.removeAssignee(user, threadId, assigneeUsername)

}
