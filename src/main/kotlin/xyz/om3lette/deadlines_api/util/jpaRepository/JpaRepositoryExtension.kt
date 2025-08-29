package xyz.om3lette.deadlines_api.util.jpaRepository

import org.springframework.data.jpa.repository.JpaRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException


inline fun <reified T, ID> JpaRepository<T, ID>.findByIdOr404(id: ID & Any): T =
    findById(id).orElseThrow {
        StatusCodeException(404, "${T::class.simpleName} $id not found")
    }
