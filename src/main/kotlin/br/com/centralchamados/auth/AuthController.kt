package br.com.centralchamados.auth

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody body: CredenciaisLogin,
        request: jakarta.servlet.http.HttpServletRequest,
    ) = authService.login(body, request.remoteAddr ?: "desconhecido")

    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody body: RefreshRequest,
    ) = authService.refresh(body.refreshToken)

    @PostMapping("/logout")
    fun logout(
        @RequestBody(required = false) body: RefreshRequest?,
    ) = authService.logout(body?.refreshToken)

    @GetMapping("/me")
    fun me(
        @AuthenticationPrincipal principal: PrincipalUsuario,
    ) = UsuarioResumo(principal.id, principal.nome, principal.email, principal.perfil, principal.provedorId)
}
