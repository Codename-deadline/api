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
            "/api/organizations/1/members",
            "/api/organizations/1/members/hints",
            "/api/organizations/1/threads",
            "/api/threads/2",
            "/api/threads/2/assignees",
            "/api/threads/2/deadlines",
            "/api/deadlines/3",
            "/api/deadlines/3/assignees",
            "/api/deadlines/3/attachments",
            "/api/attachments/4",
            "/api/attachments/4/metadata"
        ]
    )
    fun `anonymous semi-public GET continues filter chain`(uri: String) {
        val request = MockHttpServletRequest("GET", uri)
        val response = MockHttpServletResponse()
        val chain: FilterChain = mockk(relaxed = true)

        filter.doFilter(request, response, chain)

        verify(exactly = 1) { chain.doFilter(request, response) }
        verify(exactly = 0) { authenticationEntryPoint.commence(any(), any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "/api/organizations",
            "/api/threads/me",
            "/api/deadlines/me",
            "/api/attachments"
        ]
    )
    fun `user-specific and collection GET still requires authentication`(uri: String) {
        assertAuthenticationRequired("GET", uri)
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
