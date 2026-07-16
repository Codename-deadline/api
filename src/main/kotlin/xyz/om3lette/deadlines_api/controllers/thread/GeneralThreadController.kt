package xyz.om3lette.deadlines_api.controllers.thread

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Min
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.common.constraints.PaginationConstraints
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.ThreadService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/threads")
@Tag(name = "Threads")
class GeneralThreadController(
    private val threadService: ThreadService,
) {
    @GetMapping("/me")
    @Operation(summary = "Get threads which are assigned to you")
    fun getUserThreads(
        @AuthenticationPrincipal user: User,
        @RequestParam("page") @Min(PaginationConstraints.PAGE_MIN) pageNumber: Int
    ) = threadService.getThreadsByUser(user, pageNumber, 10)
}
