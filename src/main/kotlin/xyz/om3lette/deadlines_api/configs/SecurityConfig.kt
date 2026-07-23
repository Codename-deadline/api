package xyz.om3lette.deadlines_api.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RegexRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import xyz.om3lette.deadlines_api.configs.properties.CorsProperties
import xyz.om3lette.deadlines_api.entrypoints.RestAuthenticationEntryPoint
import xyz.om3lette.deadlines_api.filters.JwtAuthFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userDetailsService: UserDetailsService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtAuthFilter: JwtAuthFilter,
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint,
    private val corsProperties: CorsProperties
) {
    companion object {
        private fun api(path: String) = ApiPathPrefixConfig.API_PREFIX + path

        val PUBLIC_URLS = arrayOf(
            api("/auth/register-otp"),
            api("/auth/register-tma"),
            api("/auth/refresh-token"),
            api("/auth/verify-password"),
            api("/auth/otp"),
            api("/auth/otp/verify"),
            api("/metadata/**"),
            api("/roles/metadata"),
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/actuator/health",
        )
        val SEMI_PUBLIC_GET_URLS = arrayOf(
            Regex('^' + api("/organizations/[0-9]+(?:/.*)?") + '$'),
            Regex('^' + api("/threads/[0-9]+(?:/.*)?") + '$'),
            Regex('^' + api("/deadlines/[0-9]+(?:/.*)?") + '$'),
            Regex('^' + api("/attachments/[0-9]+(?:/.*)?") + '$'),
        )

        fun isSemiPublicGet(method: String, requestUri: String): Boolean =
            method == HttpMethod.GET.name() && SEMI_PUBLIC_GET_URLS.any { it.matches(requestUri) }
    }

    @Bean
    fun authenticationManager(): AuthenticationManager = ProviderManager(
        listOf(
            DaoAuthenticationProvider(userDetailsService).apply {
                setPasswordEncoder(passwordEncoder)
            }
        )
    )

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors {
                it.configurationSource {
                    CorsConfiguration().apply {
                        allowedHeaders = corsProperties.allowedHeaders
                        allowedMethods = corsProperties.allowedMethods
                        allowedOrigins = corsProperties.allowedOrigins
                    }
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(*PUBLIC_URLS).permitAll()
                it.requestMatchers(
                    *SEMI_PUBLIC_GET_URLS.map { pattern ->
                        RegexRequestMatcher.regexMatcher(HttpMethod.GET, pattern.pattern)
                    }.toTypedArray()
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(restAuthenticationEntryPoint)
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
