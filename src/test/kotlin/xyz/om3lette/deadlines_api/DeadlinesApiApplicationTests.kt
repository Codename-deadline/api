package xyz.om3lette.deadlines_api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import xyz.om3lette.deadlines_api.config.TestInfraMocks
import xyz.om3lette.deadlines_api.data.common.validation.MinimumValidationReason
import xyz.om3lette.deadlines_api.data.common.validation.SimpleValidationReason
import xyz.om3lette.deadlines_api.db.TestDatabaseConfig

@SpringBootTest
@ActiveProfiles("test")
@Import(TestInfraMocks::class, TestDatabaseConfig::class)
class DeadlinesApiApplicationTests {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Test
    fun contextLoads() {
    }

    @Test
    fun validationResponseIsDocumentedInOpenApi() {
        MockMvcBuilders.webAppContextSetup(context).build()
            .get("/v3/api-docs")
            .andExpect {
                status { isOk() }
                jsonPath("$.paths['/api/user/hints'].get.responses['422'].content['application/json'].schema") {
                    exists()
                }
                jsonPath("$.components.schemas.ValidationViolation.discriminator.propertyName") {
                    value("reason")
                }
                jsonPath("$.components.schemas.ValidationViolation.oneOf") {
                    isArray()
                }
                jsonPath("$.components.schemas.SimpleValidationViolation.properties.reason.enum.length()") {
                    value(SimpleValidationReason.entries.size)
                }
                jsonPath("$.components.schemas.MinimumValidationViolation.properties.reason.enum.length()") {
                    value(MinimumValidationReason.entries.size)
                }
            }
    }
}
