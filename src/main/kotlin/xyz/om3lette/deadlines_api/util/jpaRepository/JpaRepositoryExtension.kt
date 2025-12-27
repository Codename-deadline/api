package xyz.om3lette.deadlines_api.util.jpaRepository

import org.springframework.data.jpa.repository.JpaRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException


inline fun <reified T, ID> JpaRepository<T & Any, ID & Any>.findByIdOr404(id: ID & Any, errorCode: ErrorCode): T =
    findById(id).orElseThrow {
        StatusCodeException(404, errorCode)
    }
