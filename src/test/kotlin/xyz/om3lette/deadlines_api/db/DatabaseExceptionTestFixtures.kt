package xyz.om3lette.deadlines_api.db

import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import java.sql.SQLException

fun constraintViolation(constraint: DatabaseConstraint) = DataIntegrityViolationException(
    "Constraint violation: ${constraint.databaseName}",
    ConstraintViolationException("Constraint violation", SQLException(), constraint.databaseName)
)
