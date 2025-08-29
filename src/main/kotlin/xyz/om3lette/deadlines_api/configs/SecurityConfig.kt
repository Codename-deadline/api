package xyz.om3lette.deadlines_api.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.entrypoints.RestAuthenticationEntryPoint
import xyz.om3lette.deadlines_api.filters.JwtAuthFilter
import xyz.om3lette.deadlines_api.services.auth.otp.OtpAuthProvider

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userDetailsService: UserDetailsService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtAuthFilter: JwtAuthFilter,
    private val otpAuthProvider: OtpAuthProvider,
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint
) {
    companion object {
        val PUBLIC_URLS = arrayOf(
            "/api/auth/register",
            "/api/auth/register-otp",
            "/api/auth/register-tma",
            "/api/auth/sign-in",
            "/api/auth/refresh-token",
            "/api/auth/verify-password",
            "/api/auth/otp",
            "/api/auth/otp/verify",
        )
    }

    @Bean
    fun daoAuthProvider(): DaoAuthenticationProvider =
        DaoAuthenticationProvider(userDetailsService).apply {
            setPasswordEncoder(passwordEncoder)
        }

    @Bean
    fun authenticationManager(http: HttpSecurity): AuthenticationManager {
        val providers = listOf(daoAuthProvider(), otpAuthProvider)
        return ProviderManager(providers)
//        val builder = http.getSharedObject(AuthenticationManagerBuilder::class.java)
//        builder
//            .userDetailsService(userDetailsService())
//            .passwordEncoder(passwordEncoder())
//        return builder.build()
    }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
//      TODO: Enable cors
        http
            .csrf { it.disable() }
            .cors {
                it.configurationSource { request ->
                    CorsConfiguration().applyPermitDefaultValues().apply {
                        allowedHeaders = listOf("*")
                        allowedMethods = listOf("*")
                        allowedOrigins = listOf("*")
                    }
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                PUBLIC_URLS.forEach { url -> it.requestMatchers(url).permitAll() }
                it.anyRequest().authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(restAuthenticationEntryPoint)
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}