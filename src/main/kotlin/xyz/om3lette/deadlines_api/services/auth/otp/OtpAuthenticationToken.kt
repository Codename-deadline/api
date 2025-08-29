package xyz.om3lette.deadlines_api.services.auth.otp

import org.springframework.security.authentication.AbstractAuthenticationToken
import java.util.UUID

class OtpAuthenticationToken(
    val identifier: UUID,
    val code: String
) : AbstractAuthenticationToken(emptyList()){
    override fun getPrincipal(): Any = identifier

    override fun getCredentials(): Any = code
}