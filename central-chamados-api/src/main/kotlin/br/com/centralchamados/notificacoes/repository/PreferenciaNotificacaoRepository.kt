package br.com.centralchamados.notificacoes.repository

import br.com.centralchamados.notificacoes.model.PreferenciaNotificacao
import br.com.centralchamados.notificacoes.model.PreferenciaNotificacaoId
import br.com.centralchamados.shared.model.CanalNotificacao
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PreferenciaNotificacaoRepository : JpaRepository<PreferenciaNotificacao, PreferenciaNotificacaoId> {
    fun findAllByUsuarioId(usuarioId: UUID): List<PreferenciaNotificacao>

    fun findByUsuarioIdAndTipoEventoAndCanal(
        usuarioId: UUID,
        tipoEvento: String,
        canal: CanalNotificacao,
    ): PreferenciaNotificacao?
}
