package xyz.om3lette.deadlines_api.data.otp.enums

import xyz.om3lette.deadlines_api.data.user.enums.UserRole

enum class AppAuthority {
    ROLE_USER, ROLE_ADMIN, OTP_VERIFIED
    ;

    companion object {
        fun fromUserRole(role: UserRole): AppAuthority = when (role) {
            UserRole.USER -> ROLE_ADMIN
            UserRole.ADMIN -> ROLE_ADMIN
        }
    }
}
