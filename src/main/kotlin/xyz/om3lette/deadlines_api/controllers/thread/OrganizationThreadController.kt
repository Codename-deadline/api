package xyz.om3lette.deadlines_api.controllers.thread

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
import xyz.om3lette.deadlines_api.data.scopes.thread.request.CreateThreadRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.ThreadService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/organization/{organizationId}/threads")
@Tag(name = "Threads")
class OrganizationThreadController(
    val threadService: ThreadService
) {
    @GetMapping
    fun getThreads(
        @AuthenticationPrincipal user: User,
        @PathVariable organizationId: Long,
        @RequestParam("page") pageNumber: Int
    ) = threadService.getThreadsByOrganization(user, organizationId, pageNumber, 10)

    @PostMapping
    fun createThread(
        @AuthenticationPrincipal user: User,
        @PathVariable organizationId: Long,
        @RequestBody request: CreateThreadRequest
    ) = threadService.createThread(
        user,
        organizationId,
        request.title,
        request.description,
        request.usernamesToAssign
    )
}