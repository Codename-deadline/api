package xyz.om3lette.deadlines_api.util.page

import org.springframework.data.domain.Page
import xyz.om3lette.deadlines_api.data.common.response.PaginationResponse

fun <T : Any, R> Page<T>.toPaginationResponse(mapper: (T) -> R): PaginationResponse<R> =
    PaginationResponse(
        data = this.content.map(mapper),
        totalPages = this.totalPages
    )
