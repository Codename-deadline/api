package xyz.om3lette.deadlines_api.entrypoints

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.util.GeneralErrorResponse

@Component
class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val message = when (authException) {
            is CredentialsExpiredException         -> "Token has expired."
            is BadCredentialsException             -> "Invalid or expired credentials."
            is InsufficientAuthenticationException -> "Full authentication is required to access this resource"
            else                                   -> "Authentication failed."
        }

        response.contentType = "application/json"
        response.status = HttpServletResponse.SC_UNAUTHORIZED

        val error = GeneralErrorResponse(code = ErrorCode.AUTH_INVALID_CREDENTIALS, detail = message)
        response.writer.write(objectMapper.writeValueAsString(error))
    }
}