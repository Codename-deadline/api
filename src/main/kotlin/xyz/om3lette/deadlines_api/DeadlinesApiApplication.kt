package xyz.om3lette.deadlines_api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories

@SpringBootApplication
@EnableJpaRepositories(basePackages = ["xyz.om3lette.deadlines_api.data"])
@EnableRedisRepositories(basePackages= ["xyz.om3lette.deadlines_api.redisData"])
class DeadlinesApiApplication

fun main(args: Array<String>) {
	runApplication<DeadlinesApiApplication>(*args)
}
