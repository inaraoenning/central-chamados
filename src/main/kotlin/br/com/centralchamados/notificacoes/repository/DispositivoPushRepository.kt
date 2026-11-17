package br.com.centralchamados.notificacoes.repository

import br.com.centralchamados.notificacoes.model.DispositivoPush
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DispositivoPushRepository : JpaRepository<DispositivoPush, UUID> {
    fun findByTokenAndUsuarioId(
        token: String,
        usuarioId: UUID,
    ): DispositivoPush?

    fun findByUsuarioIdAndAtivoTrue(usuarioId: UUID): List<DispositivoPush>

    fun deleteByTokenAndUsuarioId(
        token: String,
        usuarioId: UUID,
    )
}
