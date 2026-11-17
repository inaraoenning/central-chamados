package br.com.centralchamados.categorias

import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/setores/{id}/categorias")
class CategoriaController(
    private val service: CategoriaService,
) {
    @GetMapping
    fun listar(
        @PathVariable id: UUID,
        pageable: Pageable,
    ) = service.listarPorSetor(id, pageable)
}
