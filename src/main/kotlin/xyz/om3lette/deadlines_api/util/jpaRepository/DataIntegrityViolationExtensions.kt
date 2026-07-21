package xyz.om3lette.deadlines_api.util.jpaRepository

import org.hibernate.exception.ConstraintViolationException
import org.postgresql.util.PSQLException
import org.springframework.dao.DataIntegrityViolationException
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint

fun DataIntegrityViolationException.violatesConstraint(constraint: DatabaseConstraint): Boolean =
    generateSequence(this as Throwable?) { it.cause }
        .any { cause ->
            when (cause) {
                is ConstraintViolationException -> cause.constraintName == constraint.databaseName
                is PSQLException -> cause.serverErrorMessage?.constraint == constraint.databaseName
                else -> false
            }
        }
