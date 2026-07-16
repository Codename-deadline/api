package xyz.om3lette.deadlines_api.exceptions.handlers

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.data.common.validation.BetweenValidationReason
import xyz.om3lette.deadlines_api.data.common.validation.BetweenValidationViolation
import xyz.om3lette.deadlines_api.data.common.validation.ExactValidationReason
import xyz.om3lette.deadlines_api.data.common.validation.ExactValidationViolation
import xyz.om3lette.deadlines_api.data.common.validation.ExpectedType
import xyz.om3lette.deadlines_api.data.common.validation.MaximumValidationReason
import xyz.om3lette.deadlines_api.data.common.validation.MaximumValidationViolation
import xyz.om3lette.deadlines_api.data.common.validation.MinimumValidationReason
import xyz.om3lette.deadlines_api.data.common.validation.MinimumValidationViolation
import xyz.om3lette.deadlines_api.data.common.validation.OneOfValidationReason
import xyz.om3lette.deadlines_api.data.common.validation.OneOfValidationViolation
import xyz.om3lette.deadlines_api.data.common.validation.SimpleValidationReason
import xyz.om3lette.deadlines_api.data.common.validation.SimpleValidationViolation
import xyz.om3lette.deadlines_api.data.common.validation.TypeValidationReason
import xyz.om3lette.deadlines_api.data.common.validation.TypeValidationViolation
import xyz.om3lette.deadlines_api.data.common.validation.ValidationErrorResponse
import xyz.om3lette.deadlines_api.data.common.validation.ValidationViolation
import xyz.om3lette.deadlines_api.data.otp.request.VerifyOtpRequest
import xyz.om3lette.deadlines_api.data.scopes.common.dto.UsernameRolePair
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.request.CreateOrganizationRequest
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.util.GeneralErrorResponse
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ValidationErrorControllerTest {
    private val jsonConverter = JacksonJsonHttpMessageConverter()
    private val objectMapper = jsonConverter.mapper
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.standaloneSetup(ValidationTestController())
            .setControllerAdvice(GlobalExceptionHandler())
            .setMessageConverters(jsonConverter)
            .build()
    }

    @Test
    fun `body size and not blank violations have exact typed JSON`() {
        mvc.post("/validation/body") {
            contentType = MediaType.APPLICATION_JSON
            content = serialized(ValidationBody(""))
        }.andExpect {
            status { isUnprocessableContent() }
            content {
                json(
                    validationResponse(
                        BetweenValidationViolation(
                            field = "name",
                            reason = BetweenValidationReason.LENGTH_BETWEEN,
                            min = 2,
                            max = 4
                        ),
                        SimpleValidationViolation("name", SimpleValidationReason.NOT_BLANK)
                    ),
                    JsonCompareMode.STRICT
                )
            }
        }
    }

    @Test
    fun `native method parameter validation is normalized`() {
        mvc.get("/validation/method") { param("page", "0") }
            .andExpect {
                status { isUnprocessableContent() }
                content {
                    json(
                        validationResponse(
                            MinimumValidationViolation("page", MinimumValidationReason.VALUE_MIN, 1)
                        ),
                        JsonCompareMode.STRICT
                    )
                }
            }
    }

    @Test
    fun `semantic pattern does not expose its regex`() {
        mvc.post("/validation/pattern") {
            contentType = MediaType.APPLICATION_JSON
            content = serialized(VerifyOtpRequest(VALID_ID, "abc123"))
        }.andExpect {
            status { isUnprocessableContent() }
            content {
                json(
                    validationResponse(
                        SimpleValidationViolation("code", SimpleValidationReason.FORMAT_DIGITS_ONLY)
                    ),
                    JsonCompareMode.STRICT
                )
            }
        }

        mvc.post("/validation/pattern") {
            contentType = MediaType.APPLICATION_JSON
            content = serialized(VerifyOtpRequest(VALID_ID, "12345"))
        }.andExpect {
            status { isUnprocessableContent() }
            content {
                json(
                    validationResponse(
                        ExactValidationViolation("code", ExactValidationReason.LENGTH_EXACT, 6)
                    ),
                    JsonCompareMode.STRICT
                )
            }
        }
    }

    @Test
    fun `nested invitation path matches client JSON`() {
        mvc.post("/validation/nested") {
            contentType = MediaType.APPLICATION_JSON
            content = serialized(
                CreateOrganizationRequest(
                    title = "valid",
                    description = null,
                    type = OrganizationType.PUBLIC,
                    invitations = listOf(UsernameRolePair("", ScopeRole.ORG_MEMBER))
                )
            )
        }.andExpect {
            status { isUnprocessableContent() }
            content {
                json(
                    validationResponse(
                        BetweenValidationViolation(
                            field = "invitations[0].username",
                            reason = BetweenValidationReason.LENGTH_BETWEEN,
                            min = 3,
                            max = 32
                        ),
                        SimpleValidationViolation(
                            "invitations[0].username",
                            SimpleValidationReason.NOT_BLANK
                        )
                    ),
                    JsonCompareMode.STRICT
                )
            }
        }
    }

    @Test
    fun `binding failures use semantic variants`() {
        mvc.get("/validation/method") { param("page", "not-a-number") }
            .andExpect {
                status { isUnprocessableContent() }
                content {
                    json(
                        validationResponse(TypeValidationViolation("page", expected = ExpectedType.INTEGER)),
                        JsonCompareMode.STRICT
                    )
                }
            }

        mvc.get("/validation/method")
            .andExpect {
                status { isUnprocessableContent() }
                content {
                    json(
                        validationResponse(
                            SimpleValidationViolation("page", SimpleValidationReason.REQUIRED)
                        ),
                        JsonCompareMode.STRICT
                    )
                }
            }
    }

    @Test
    fun `Jackson failures use semantic variants`() {
        assertDeserializationViolation(
            DeserializationPayload(id = "not-a-uuid"),
            SimpleValidationViolation("id", SimpleValidationReason.FORMAT_UUID)
        )
        assertDeserializationViolation(
            DeserializationPayload(due = "not-a-time"),
            SimpleValidationViolation("due", SimpleValidationReason.FORMAT_TIMESTAMP)
        )
        assertDeserializationViolation(
            DeserializationPayload(role = "NOT_A_ROLE"),
            OneOfValidationViolation("role", allowed = ScopeRole.entries.map { it.name })
        )

        mvc.post("/validation/deserialize") {
            contentType = MediaType.APPLICATION_JSON
            content = serialized(DeserializationPayloadWithoutRequired())
        }.andExpect {
            status { isUnprocessableContent() }
            content {
                json(
                    validationResponse(
                        SimpleValidationViolation("required", SimpleValidationReason.REQUIRED)
                    ),
                    JsonCompareMode.STRICT
                )
            }
        }

        mvc.post("/validation/deserialize") {
            contentType = MediaType.APPLICATION_JSON
            content = serialized(DeserializationPayload(required = null))
        }.andExpect {
            status { isUnprocessableContent() }
            content {
                json(
                    validationResponse(
                        SimpleValidationViolation("required", SimpleValidationReason.REQUIRED)
                    ),
                    JsonCompareMode.STRICT
                )
            }
        }

        mvc.post("/validation/deserialize") {
            contentType = MediaType.APPLICATION_JSON
            content = serialized(DeserializationPayload(required = emptyList<Nothing>()))
        }.andExpect {
            status { isUnprocessableContent() }
            content {
                json(
                    validationResponse(TypeValidationViolation("required", expected = ExpectedType.STRING)),
                    JsonCompareMode.STRICT
                )
            }
        }

        mvc.post("/validation/deserialize") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"id":""""
        }.andExpect {
            status { isUnprocessableContent() }
            content {
                json(
                    serialized(GeneralErrorResponse(code = ErrorCode.DESERIALIZATION_ERROR)),
                    JsonCompareMode.STRICT
                )
            }
        }
    }

    @Test
    fun `OpenAPI hierarchy declares reason discriminator and every wire shape`() {
        val schema = ValidationViolation::class.java.getAnnotation(Schema::class.java)
        assertEquals("reason", schema.discriminatorProperty)
        assertEquals(
            setOf(
                SimpleValidationViolation::class,
                MinimumValidationViolation::class,
                MaximumValidationViolation::class,
                BetweenValidationViolation::class,
                ExactValidationViolation::class,
                OneOfValidationViolation::class,
                TypeValidationViolation::class
            ),
            schema.oneOf.toSet()
        )
        val expectedReasons = buildSet {
            addAll(SimpleValidationReason.entries.map { it.code })
            addAll(MinimumValidationReason.entries.map { it.code })
            addAll(MaximumValidationReason.entries.map { it.code })
            addAll(BetweenValidationReason.entries.map { it.code })
            addAll(ExactValidationReason.entries.map { it.code })
            addAll(OneOfValidationReason.entries.map { it.code })
            addAll(TypeValidationReason.entries.map { it.code })
        }
        assertEquals(expectedReasons, schema.discriminatorMapping.map { it.value }.toSet())
    }

    private fun assertDeserializationViolation(
        request: DeserializationPayload,
        violation: ValidationViolation
    ) {
        mvc.post("/validation/deserialize") {
            contentType = MediaType.APPLICATION_JSON
            content = serialized(request)
        }.andExpect {
            status { isUnprocessableContent() }
            content {
                json(validationResponse(violation), JsonCompareMode.STRICT)
            }
        }
    }

    private fun validationResponse(vararg violations: ValidationViolation): String =
        serialized(ValidationErrorResponse(violations = violations.toList()))

    private fun serialized(value: Any): String = objectMapper.writeValueAsString(value)
}

private val VALID_ID: UUID = UUID.randomUUID()
private val VALID_DUE: Instant = Instant.now()

@RestController
private class ValidationTestController {
    @PostMapping("/validation/body")
    fun body(@Valid @RequestBody request: ValidationBody) = request

    @GetMapping("/validation/method")
    fun method(@RequestParam("page") @Min(1) page: Int) = page

    @PostMapping("/validation/pattern")
    fun pattern(@Valid @RequestBody request: VerifyOtpRequest) = request

    @PostMapping("/validation/nested")
    fun nested(@Valid @RequestBody request: CreateOrganizationRequest) = request

    @PostMapping("/validation/deserialize")
    fun deserialize(@RequestBody request: DeserializationBody) = request
}

private data class ValidationBody(
    @field:NotBlank
    @field:Size(min = 2, max = 4)
    val name: String
)

private data class DeserializationBody(
    val id: UUID,
    val due: Instant,
    val role: ScopeRole,
    val required: String
)

private data class DeserializationPayload(
    val id: Any = VALID_ID,
    val due: Any = VALID_DUE,
    val role: Any = ScopeRole.ORG_MEMBER,
    val required: Any? = "present"
)

private data class DeserializationPayloadWithoutRequired(
    val id: UUID = VALID_ID,
    val due: Instant = VALID_DUE,
    val role: ScopeRole = ScopeRole.ORG_MEMBER
)
