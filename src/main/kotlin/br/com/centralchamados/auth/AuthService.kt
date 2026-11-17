package br.com.centralchamados.auth

import br.com.centralchamados.auth.model.SessaoRefresh
import br.com.centralchamados.auth.repository.SessaoRefreshRepository
import br.com.centralchamados.shared.api.ErroApi
import br.com.centralchamados.usuarios.model.Usuario
import br.com.centralchamados.usuarios.repository.UsuarioRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class AuthService(
    private val usuarios: UsuarioRepository,
    private val sessoes: SessaoRefreshRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    private val tentativasPorIp = ConcurrentHashMap<String, MutableList<Instant>>()

    @Transactional
    fun login(
        credenciais: CredenciaisLogin,
        ip: String,
    ): TokensResposta {
        val agora = Instant.now()
        val tentativas = tentativasPorIp.computeIfAbsent(ip) { mutableListOf() }
        synchronized(tentativas) {
            tentativas.removeIf { it.isBefore(agora.minusSeconds(60)) }
            if (tentativas.size >= 5) throw ErroApi(HttpStatus.TOO_MANY_REQUESTS, "LIMITE_LOGIN", "Tente novamente em instantes")
            tentativas.add(agora)
        }
        val usuario = usuarios.findByEmailIgnoreCase(credenciais.email)
        if (usuario == null || !usuario.ativo || !passwordEncoder.matches(credenciais.senha, usuario.senhaHash)) {
            throw ErroApi(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos")
        }
        return emitirTokens(usuario)
    }

    @Transactional
    fun refresh(token: String): TokensResposta {
        val sessao =
            sessoes.findByHashAndRevogadaEmIsNull(jwtService.hash(token))
                ?: throw ErroApi(HttpStatus.UNAUTHORIZED, "REFRESH_INVALIDO", "Refresh token inválido")
        if (sessao.expiraEm.isBefore(Instant.now()) || !sessao.usuario.ativo) {
            sessao.revogadaEm = Instant.now()
            throw ErroApi(HttpStatus.UNAUTHORIZED, "REFRESH_EXPIRADO", "Refresh token expirado")
        }
        sessao.revogadaEm = Instant.now()
        return emitirTokens(sessao.usuario)
    }

    @Transactional
    fun logout(token: String?) {
        if (!token.isNullOrBlank()) {
            sessoes.findByHashAndRevogadaEmIsNull(jwtService.hash(token))?.revogadaEm = Instant.now()
        }
    }

    @Transactional(readOnly = true)
    fun validarAccessToken(token: String): PrincipalUsuario? {
        val id = jwtService.validar(token) ?: return null
        val usuario = usuarios.findById(id).orElse(null) ?: return null
        if (!usuario.ativo) return null
        return usuario.principal()
    }

    private fun emitirTokens(usuario: Usuario): TokensResposta {
        val refresh = jwtService.novoRefreshToken()
        sessoes.save(SessaoRefresh(usuario = usuario, hash = jwtService.hash(refresh), expiraEm = jwtService.expiraRefresh()))
        return TokensResposta(jwtService.accessToken(usuario), refresh, expiresIn = jwtService.duracaoAccessSegundos())
    }

    private fun Usuario.principal() = PrincipalUsuario(id, nome, email, perfil, provedor?.id, setores.map { it.id }.toSet())
}
