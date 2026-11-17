package br.com.centralchamados.notificacoes

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/configuracoes-notificacao")
@PreAuthorize("hasRole('ADMIN')")
class AdminConfiguracaoNotificacaoController(
    private val service: ConfiguracaoNotificacaoService,
) {
    @GetMapping
    fun listar() = service.listar()

    @PutMapping
    fun atualizar(
        @Valid @RequestBody body: List<ConfiguracaoRequest>,
    ) = service.atualizar(body)
}
