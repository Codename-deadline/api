package xyz.om3lette.deadlines_api.data.common.response

import org.springframework.data.domain.Page

data class PaginationResponse<T>(
    val data: List<T>,
    val totalPages: Int
) {
    companion object {
        fun <T : Any> fromPage(page: Page<T>) = PaginationResponse(page.toList(), page.totalPages)
    }
}
