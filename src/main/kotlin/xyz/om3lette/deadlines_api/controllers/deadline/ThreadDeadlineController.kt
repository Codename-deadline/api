package xyz.om3lette.deadlines_api.controllers.deadline

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.common.constraints.PaginationConstraints
import xyz.om3lette.deadlines_api.data.scopes.deadline.requests.CreateDeadlineRequest
import xyz.om3lette.deadlines_api.data.scopes.common.dto.UsernameRolePairList
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.DeadlineService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/threads/{threadId}/deadlines")
@Tag(name = "Deadlines")
class ThreadDeadlineController(
    val deadlineService: DeadlineService
) {
    @GetMapping
    @Operation(summary = "Get all deadlines threads")
    fun getDeadlines(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive threadId: Long,
        @RequestParam("page") @Min(PaginationConstraints.PAGE_MIN) pageNumber: Int
    ) = deadlineService.getDeadlinesByThread(user, threadId, pageNumber, 10)

    @PostMapping
    @Operation(summary = "Create new deadline")
    fun createDeadline(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive threadId: Long,
        @Valid @RequestBody request: CreateDeadlineRequest
    ) = deadlineService.createDeadline(
        user,
        threadId,
        request.title,
        request.description,
        request.due,
        UsernameRolePairList(request.invitations)
    )
}
