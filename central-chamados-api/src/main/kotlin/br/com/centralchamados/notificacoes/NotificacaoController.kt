package br.com.centralchamados.notificacoes

import br.com.centralchamados.auth.PrincipalUsuario
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notificacoes")
class NotificacaoController(
    private val service: NotificacaoService,
    private val sse: NotificacaoSse,
) {
    @GetMapping
    fun listar(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @RequestParam(required = false) lida: Boolean?,
        pageable: Pageable,
    ) = service.listar(principal, lida, pageable)

    @GetMapping("/nao-lidas/contagem")
    fun contagem(
        @AuthenticationPrincipal principal: PrincipalUsuario,
    ) = service.contagem(principal)

    @PatchMapping("/{id}/lida")
    fun marcarLida(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @PathVariable id: UUID,
    ) = service.marcarLida(principal, id)

    @PatchMapping("/marcar-todas-lidas")
    fun marcarTodas(
        @AuthenticationPrincipal principal: PrincipalUsuario,
    ) = service.marcarTodasLidas(principal)

    @GetMapping("/stream")
    fun stream(
        @AuthenticationPrincipal principal: PrincipalUsuario,
    ) = sse.conectar(principal.id)

    @GetMapping("/preferencias")
    fun preferencias(
        @AuthenticationPrincipal principal: PrincipalUsuario,
    ) = service.listarPreferencias(principal)

    @PutMapping("/preferencias")
    fun atualizarPreferencias(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @Valid @RequestBody body: List<PreferenciaRequest>,
    ) = service.atualizarPreferencias(principal, body)
}
