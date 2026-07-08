package xyz.om3lette.deadlines_api.data.otp.constraints

object OtpConstraints {
    const val CODE_LENGTH = 6
    const val IDENTIFIER_MAX = 255
    const val NUMERIC_IDENTIFIER_PATTERN = "\\d{1,255}"
    const val TMA_INIT_DATA_MAX = 4096
}
