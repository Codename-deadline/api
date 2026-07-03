package xyz.om3lette.deadlines_api.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import xyz.om3lette.deadlines_api.configs.properties.RedisProperties


@Configuration
class RedisConfig(
    private val redisProperties: RedisProperties
) {
    @Bean
    fun jedisConnectionFactory(): JedisConnectionFactory =
        JedisConnectionFactory(
            RedisStandaloneConfiguration().apply {
                hostName = redisProperties.hostname
                port = redisProperties.port
            }
        )

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>().apply {
            connectionFactory = jedisConnectionFactory()
        }
}
