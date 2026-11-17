package br.com.centralchamados.setores.repository

import br.com.centralchamados.setores.model.Setor
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SetorRepository : JpaRepository<Setor, UUID> {
    fun findByAtivoTrue(pageable: Pageable): Page<Setor>
}
