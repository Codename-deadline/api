package xyz.om3lette.deadlines_api.controllers.deadline

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.scopes.deadline.requests.CreateDeadlineRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.DeadlineService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/threads/{threadId}/deadlines")
@Tag(name = "Deadlines")
class ThreadDeadlineController(
    val deadlineService: DeadlineService
) {
    @GetMapping
    fun getDeadlines(
        @AuthenticationPrincipal user: User,
        @PathVariable threadId: Long,
        @RequestParam("page") pageNumber: Int
    ) = deadlineService.getDeadlinesByThread(user, threadId, pageNumber, 10)

    @PostMapping
    fun createDeadline(
        @AuthenticationPrincipal user: User,
        @PathVariable threadId: Long,
        @RequestBody request: CreateDeadlineRequest
    ) = deadlineService.createDeadline(
        user,
        threadId,
        request.title,
        request.description,
        request.due,
        request.status,
        request.usernamesToAssign
    )
}
