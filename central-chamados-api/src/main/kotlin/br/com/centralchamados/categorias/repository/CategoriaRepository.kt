package br.com.centralchamados.categorias.repository

import br.com.centralchamados.categorias.model.Categoria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CategoriaRepository : JpaRepository<Categoria, UUID> {
    fun findBySetorIdAndNomeAndAtivoTrue(
        setorId: UUID,
        nome: String,
    ): Categoria?

    fun findBySetorIdAndAtivoTrue(
        setorId: UUID,
        pageable: Pageable,
    ): Page<Categoria>
}
