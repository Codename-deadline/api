package xyz.om3lette.deadlines_api.filters

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.entrypoints.RestAuthenticationEntryPoint
import xyz.om3lette.deadlines_api.services.JwtService

class JwtAuthFilterTest {
    private val jwtService: JwtService = mockk()
    private val userRepository: UserRepository = mockk()
    private val authenticationEntryPoint: RestAuthenticationEntryPoint = mockk()
    private val filter = JwtAuthFilter(jwtService, userRepository, authenticationEntryPoint)

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/api/organizations/1",
            "/api/threads/2",
            "/api/deadlines/3"
        ]
    )
    fun `anonymous exact semi-public GET continues filter chain`(uri: String) {
        val request = MockHttpServletRequest("GET", uri)
        val response = MockHttpServletResponse()
        val chain: FilterChain = mockk(relaxed = true)

        filter.doFilter(request, response, chain)

        verify(exactly = 1) { chain.doFilter(request, response) }
        verify(exactly = 0) { authenticationEntryPoint.commence(any(), any(), any()) }
    }

    @Test
    fun `nested route still requires authentication`() {
        assertAuthenticationRequired("GET", "/api/organizations/1/members")
    }

    @Test
    fun `non-GET route still requires authentication`() {
        assertAuthenticationRequired("PATCH", "/api/organizations/1")
    }

    private fun assertAuthenticationRequired(method: String, uri: String) {
        val request = MockHttpServletRequest(method, uri)
        val response = MockHttpServletResponse()
        val chain: FilterChain = mockk(relaxed = true)
        every { authenticationEntryPoint.commence(any(), any(), any()) } just Runs

        filter.doFilter(request, response, chain)

        verify(exactly = 0) { chain.doFilter(any(), any()) }
        verify(exactly = 1) { authenticationEntryPoint.commence(request, response, any()) }
    }
}
