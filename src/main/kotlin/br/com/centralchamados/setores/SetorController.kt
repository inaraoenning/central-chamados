package br.com.centralchamados.setores

import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/setores")
class SetorController(
    private val service: SetorService,
) {
    @GetMapping
    fun listar(pageable: Pageable) = service.listarAtivos(pageable)
}
