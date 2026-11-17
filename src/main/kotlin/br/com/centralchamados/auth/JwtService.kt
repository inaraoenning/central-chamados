package br.com.centralchamados.auth

import br.com.centralchamados.usuarios.model.Usuario
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.access-minutes:15}") private val accessMinutes: Long,
    @Value("\${app.jwt.refresh-days:30}") private val refreshDays: Long,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    private val secureRandom = SecureRandom()

    fun accessToken(usuario: Usuario): String {
        val agora = Instant.now()
        return Jwts
            .builder()
            .subject(usuario.id.toString())
            .claim("perfil", usuario.perfil.name)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plus(accessMinutes, ChronoUnit.MINUTES)))
            .signWith(key)
            .compact()
    }

    fun validar(token: String): UUID? =
        runCatching {
            UUID.fromString(
                Jwts
                    .parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload.subject,
            )
        }.getOrNull()

    fun novoRefreshToken(): String {
        val token = ByteArray(32)
        secureRandom.nextBytes(token)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token)
    }

    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }

    fun expiraRefresh(): Instant = Instant.now().plus(refreshDays, ChronoUnit.DAYS)

    fun duracaoAccessSegundos(): Long = accessMinutes * 60
}
