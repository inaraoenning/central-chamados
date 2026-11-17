package br.com.centralchamados.notificacoes

import br.com.centralchamados.auth.PrincipalUsuario
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dispositivos")
class DispositivoPushController(
    private val service: DispositivoPushService,
) {
    @PostMapping
    fun registrar(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @Valid @RequestBody body: DispositivoRequest,
    ) = service.registrar(principal.id, body)

    @DeleteMapping("/{token}")
    fun remover(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @PathVariable token: String,
    ) = service.remover(principal.id, token)
}
