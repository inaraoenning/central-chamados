package br.com.centralchamados.auth

data class TokensResposta(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)
