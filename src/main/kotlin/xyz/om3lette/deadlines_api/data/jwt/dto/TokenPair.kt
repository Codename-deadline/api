package xyz.om3lette.deadlines_api.data.jwt.dto

data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)
