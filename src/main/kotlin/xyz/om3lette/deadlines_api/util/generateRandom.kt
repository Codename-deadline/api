package xyz.om3lette.deadlines_api.util

import java.security.SecureRandom
import java.util.Base64

fun generateNumericCode(length: Int): String {
    val random = SecureRandom()
    val stringBuilder = StringBuilder(length)
    repeat(length) {
        stringBuilder.append(random.nextInt(10))
    }
    return stringBuilder.toString()
}