package br.com.centralchamados.notificacoes.repository

import br.com.centralchamados.notificacoes.model.Notificacao
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NotificacaoRepository : JpaRepository<Notificacao, UUID> {
    fun findByDestinatarioId(
        destinatarioId: UUID,
        pageable: Pageable,
    ): Page<Notificacao>

    fun findByDestinatarioIdAndLida(
        destinatarioId: UUID,
        lida: Boolean,
        pageable: Pageable,
    ): Page<Notificacao>

    fun countByDestinatarioIdAndLidaFalse(destinatarioId: UUID): Long

    fun findByIdAndDestinatarioId(
        id: UUID,
        destinatarioId: UUID,
    ): Notificacao?
}
