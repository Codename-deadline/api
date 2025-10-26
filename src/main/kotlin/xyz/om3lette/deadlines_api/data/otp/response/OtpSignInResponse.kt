package xyz.om3lette.deadlines_api.data.otp.response

import xyz.om3lette.deadlines_api.data.jwt.dto.TokenPair
import java.util.UUID

sealed class OtpSignInResponse {
    data class OK(
        val tokenPair: TokenPair,

        val passwordRequired: Boolean = false
    ) : OtpSignInResponse()

    data class PasswordRequired(
        val requestId: UUID,

        val passwordRequired: Boolean = true
    ) : OtpSignInResponse()
}