package xyz.om3lette.deadlines_api.filters

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import xyz.om3lette.deadlines_api.configs.SecurityConfig
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.entrypoints.RestAuthenticationEntryPoint
import xyz.om3lette.deadlines_api.services.JwtService

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (SecurityConfig.PUBLIC_URLS.any { request.requestURI.startsWith(it.substringBefore("/**")) })
            return filterChain.doFilter(request, response)

        try {
            val authHeader = request.getHeader("Authorization")
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw BadCredentialsException("")
            }

            val jwt = authHeader.substring(7)

            val claims = try {
                jwtService.extractAllClaims(jwt)
            } catch (ex: ExpiredJwtException) {
                throw CredentialsExpiredException("", ex)
            } catch (ex: JwtException) {
                throw BadCredentialsException("", ex)
            }

            val username: String? = claims.subject
            val isRefresh: Boolean = (claims["refresh"] ?: false) as Boolean

            if (isRefresh || username == null) {
                throw BadCredentialsException("")
            }

            val user: User = userRepository.findByUsernameIgnoreCase(username).orElseThrow { BadCredentialsException("") }

            if (!jwtService.isTokenValid(jwt, username)) {
                throw BadCredentialsException("")
            }

            if (SecurityContextHolder.getContext().authentication == null) {
                val authToken = UsernamePasswordAuthenticationToken(user, null, user.authorities)
                SecurityContextHolder.getContext().authentication = authToken
            }
            filterChain.doFilter(request, response)
        } catch (error: AuthenticationException) {
            restAuthenticationEntryPoint.commence(request, response, error)
        }
    }
}