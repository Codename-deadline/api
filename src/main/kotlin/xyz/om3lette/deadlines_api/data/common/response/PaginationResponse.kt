package xyz.om3lette.deadlines_api.data.common.response

data class PaginationResponse<T>(
    val data: List<T>,
    val totalPages: Int
)
