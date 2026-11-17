package br.com.centralchamados.auth.repository

import br.com.centralchamados.auth.model.SessaoRefresh
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SessaoRefreshRepository : JpaRepository<SessaoRefresh, UUID> {
    fun findByHashAndRevogadaEmIsNull(hash: String): SessaoRefresh?
}
