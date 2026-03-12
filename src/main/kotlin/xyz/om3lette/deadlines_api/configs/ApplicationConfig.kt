package xyz.om3lette.deadlines_api.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import tools.jackson.databind.ObjectMapper
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository

@Configuration
@EnableScheduling
class ApplicationConfig(
    private val userRepository: UserRepository
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(): UserDetailsService =
        UserDetailsService { username ->
            userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow { UsernameNotFoundException("User not found") }
        }

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper()
}