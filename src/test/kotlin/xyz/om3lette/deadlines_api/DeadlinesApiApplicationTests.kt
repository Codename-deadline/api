package xyz.om3lette.deadlines_api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import xyz.om3lette.deadlines_api.config.TestInfraMocks

@SpringBootTest
@ActiveProfiles("test")
@Import(TestInfraMocks::class)
class DeadlinesApiApplicationTests {

	@Test
	fun contextLoads() {
	}

}
