package br.com.centralchamados.chamados

import br.com.centralchamados.auditoria.AuditoriaService
import br.com.centralchamados.auth.PrincipalUsuario
import br.com.centralchamados.shared.model.Prioridade
import br.com.centralchamados.shared.model.StatusChamado
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/chamados")
class ChamadoController(
    private val service: ChamadoService,
    private val auditoria: AuditoriaService,
) {
    @PostMapping
    fun abrir(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @Valid @RequestBody body: AbrirChamadoRequest,
    ) = service.abrir(principal, body)

    @GetMapping
    fun listar(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @RequestParam(required = false) status: StatusChamado?,
        @RequestParam(required = false) prioridade: Prioridade?,
        @RequestParam(required = false) setorId: UUID?,
        @RequestParam(required = false) categoriaId: UUID?,
        @RequestParam(required = false) provedorId: UUID?,
        @RequestParam(required = false) atendenteId: UUID?,
        @RequestParam(required = false) de: Instant?,
        @RequestParam(required = false) ate: Instant?,
        @RequestParam(required = false) busca: String?,
        pageable: Pageable,
    ) = service.listar(
        principal,
        ChamadoFiltros(status, prioridade, setorId, categoriaId, provedorId, atendenteId, de, ate, busca),
        pageable,
    )

    @GetMapping("/meus")
    fun meus(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        pageable: Pageable,
    ) = service.meus(principal, pageable)

    @GetMapping("/fila")
    fun fila(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        pageable: Pageable,
    ) = service.fila(principal, pageable)

    @GetMapping("/{id}")
    fun obter(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @PathVariable id: UUID,
    ) = service.obter(principal, id)

    @PostMapping("/{id}/assumir")
    fun assumir(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @PathVariable id: UUID,
        @Valid @RequestBody body: AssumirChamadoRequest,
    ) = service.assumir(principal, id, body)

    @PatchMapping("/{id}/prioridade")
    fun prioridade(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @PathVariable id: UUID,
        @Valid @RequestBody body: AlterarPrioridadeRequest,
    ) = service.alterarPrioridade(principal, id, body)

    @PatchMapping("/{id}/previsao")
    fun previsao(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @PathVariable id: UUID,
        @Valid @RequestBody body: AlterarPrevisaoRequest,
    ) = service.alterarPrevisao(principal, id, body)

    @PostMapping("/{id}/concluir")
    fun concluir(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @PathVariable id: UUID,
        @Valid @RequestBody body: ConcluirChamadoRequest,
    ) = service.concluir(principal, id, body)

    @GetMapping("/{id}/logs")
    fun logs(
        @AuthenticationPrincipal principal: PrincipalUsuario,
        @PathVariable id: UUID,
    ) = auditoria.listarDoChamado(principal, id)
}
