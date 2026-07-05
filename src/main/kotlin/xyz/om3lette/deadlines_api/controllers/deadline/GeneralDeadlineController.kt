package xyz.om3lette.deadlines_api.controllers.deadline

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.DeadlineService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/deadlines")
@Tag(name = "Deadlines")
class GeneralDeadlineController(
    private val deadlineService: DeadlineService,
) {
    @GetMapping("/me")
    @Operation(summary = "Get deadlines which are assigned to you")
    fun getUserDeadlines(
        @AuthenticationPrincipal user: User,
        @RequestParam("page") pageNumber: Int
    ) = deadlineService.getDeadlinesByUser(user, pageNumber, 10)
}
