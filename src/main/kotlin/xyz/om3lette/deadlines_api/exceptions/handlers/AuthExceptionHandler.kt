package xyz.om3lette.deadlines_api.exceptions.handlers

import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler


@Order(Ordered.LOWEST_PRECEDENCE - 1)
@ControllerAdvice
class AuthExceptionHandler {
    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(error: BadCredentialsException, response: HttpServletResponse) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired credentials.")
    }

    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredJwt(ex: ExpiredJwtException, response: HttpServletResponse) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired.")
    }
}
