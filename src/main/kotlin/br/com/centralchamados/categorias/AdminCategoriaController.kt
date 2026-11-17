package br.com.centralchamados.categorias

import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/admin/categorias")
@PreAuthorize("hasRole('ADMIN')")
class AdminCategoriaController(
    private val service: CategoriaService,
) {
    @GetMapping
    fun listar(pageable: Pageable) = service.listarAdmin(pageable)

    @PostMapping
    fun criar(
        @Valid @RequestBody body: CategoriaRequest,
    ) = service.criar(body)

    @PutMapping("/{id}")
    fun atualizar(
        @PathVariable id: UUID,
        @Valid @RequestBody body: CategoriaRequest,
    ) = service.atualizar(id, body)

    @DeleteMapping("/{id}")
    fun desativar(
        @PathVariable id: UUID,
    ) = service.desativar(id)
}
