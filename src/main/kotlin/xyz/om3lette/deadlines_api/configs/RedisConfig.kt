package xyz.om3lette.deadlines_api.configs

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate


@Configuration
class RedisConfig(
    @param:Value("\${spring.redis.hostname}") val redisHostname: String,
    @param:Value("\${spring.redis.port}") val redisPort: Int
) {
    @Bean
    fun jedisConnectionFactory(): JedisConnectionFactory =
        JedisConnectionFactory(
            RedisStandaloneConfiguration().apply {
                hostName = redisHostname
                port = redisPort
            }
        )

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any> =
        RedisTemplate<String, Any>().apply {
            connectionFactory = jedisConnectionFactory()
        }
}
