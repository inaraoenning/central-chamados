package br.com.centralchamados.notificacoes.repository

import br.com.centralchamados.notificacoes.model.NotificacaoEnvio
import br.com.centralchamados.shared.model.StatusEnvio
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface NotificacaoEnvioRepository : JpaRepository<NotificacaoEnvio, UUID> {
    @Query(
        "select e from NotificacaoEnvio e where e.status = :status and e.tentativas < :tentativas and (e.proximaTentativaEm is null or e.proximaTentativaEm <= :agora) order by e.id",
    )
    fun pendentes(
        @Param("status") status: StatusEnvio,
        @Param("tentativas") tentativas: Int,
        @Param("agora") agora: Instant,
        pageable: Pageable,
    ): List<NotificacaoEnvio>
}
