package br.com.centralchamados.auditoria

import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/logs")
@PreAuthorize("hasRole('ADMIN')")
class AdminAuditoriaController(
    private val service: AuditoriaService,
) {
    @GetMapping
    fun listar(
        @RequestParam(required = false) chamadoId: UUID?,
        @RequestParam(required = false) autorId: UUID?,
        @RequestParam(required = false) tipoEvento: String?,
        @RequestParam(required = false) de: Instant?,
        @RequestParam(required = false) ate: Instant?,
        pageable: Pageable,
    ) = service.listarAdmin(chamadoId, autorId, tipoEvento, de, ate, pageable)
}
