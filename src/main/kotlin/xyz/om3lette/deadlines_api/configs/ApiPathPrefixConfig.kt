package xyz.om3lette.deadlines_api.configs

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ApiPathPrefixConfig : WebMvcConfigurer {
    companion object {
        const val API_PREFIX = "/api"
    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix(
            API_PREFIX,
            HandlerTypePredicate.forBasePackage("xyz.om3lette.deadlines_api.controllers")
        )
    }
}
