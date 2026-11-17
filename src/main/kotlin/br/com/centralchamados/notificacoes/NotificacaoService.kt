package br.com.centralchamados.notificacoes

import br.com.centralchamados.auth.PrincipalUsuario
import br.com.centralchamados.chamados.model.Chamado
import br.com.centralchamados.notificacoes.model.Notificacao
import br.com.centralchamados.notificacoes.model.NotificacaoEnvio
import br.com.centralchamados.notificacoes.model.PreferenciaNotificacao
import br.com.centralchamados.notificacoes.model.PreferenciaNotificacaoId
import br.com.centralchamados.notificacoes.repository.NotificacaoEnvioRepository
import br.com.centralchamados.notificacoes.repository.NotificacaoRepository
import br.com.centralchamados.notificacoes.repository.PreferenciaNotificacaoRepository
import br.com.centralchamados.shared.api.ErroApi
import br.com.centralchamados.shared.model.CanalNotificacao
import br.com.centralchamados.usuarios.model.Usuario
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class NotificacaoService(
    private val notificacoes: NotificacaoRepository,
    private val envios: NotificacaoEnvioRepository,
    private val preferencias: PreferenciaNotificacaoRepository,
    private val publisher: ApplicationEventPublisher,
    private val jdbc: JdbcTemplate,
) {
    fun notificar(
        destinatario: Usuario,
        chamado: Chamado,
        tipoEvento: String,
        titulo: String,
        mensagem: String,
        autorId: UUID? = null,
        alertaAgendado: Boolean = false,
    ): Boolean {
        if (destinatario.id == autorId) return false
        val id = UUID.randomUUID()
        val not =
            Notificacao(
                id = id,
                destinatario = destinatario,
                chamado = chamado,
                tipoEvento = tipoEvento,
                titulo = titulo,
                mensagem = mensagem,
                alertaAgendado = alertaAgendado,
            )
        if (alertaAgendado) {
            val inseridos =
                jdbc.update(
                    """INSERT INTO notificacao(id, destinatario_id, chamado_id, tipo_evento, titulo, mensagem, alerta_agendado)
                   VALUES (?, ?, ?, ?, ?, ?, TRUE)
                   ON CONFLICT (chamado_id, tipo_evento, destinatario_id) WHERE alerta_agendado DO NOTHING""",
                    id,
                    destinatario.id,
                    chamado.id,
                    tipoEvento,
                    titulo,
                    mensagem,
                )
            if (inseridos == 0) return false
        }
        val persistida = if (alertaAgendado) notificacoes.findById(id).orElseThrow() else notificacoes.save(not)
        CanalNotificacao.entries.forEach { canal ->
            val preferencia = preferencias.findByUsuarioIdAndTipoEventoAndCanal(destinatario.id, tipoEvento, canal)
            if (preferencia?.ativo != false) {
                envios.save(NotificacaoEnvio(notificacao = persistida, canal = canal))
            }
        }
        publisher.publishEvent(NotificacaoCriada(destinatario.id, id))
        return true
    }

    @Transactional(readOnly = true)
    fun listar(
        principal: PrincipalUsuario,
        lida: Boolean?,
        pageable: Pageable,
    ): Page<NotificacaoResposta> =
        (
            if (lida == null) {
                notificacoes.findByDestinatarioId(principal.id, pageable)
            } else {
                notificacoes.findByDestinatarioIdAndLida(principal.id, lida, pageable)
            }
        ).map { it.toResposta() }

    @Transactional(readOnly = true)
    fun contagem(principal: PrincipalUsuario): Map<String, Long> =
        mapOf("naoLidas" to notificacoes.countByDestinatarioIdAndLidaFalse(principal.id))

    @Transactional
    fun marcarLida(
        principal: PrincipalUsuario,
        id: UUID,
    ) {
        val notificacao =
            notificacoes.findByIdAndDestinatarioId(id, principal.id)
                ?: throw ErroApi(HttpStatus.NOT_FOUND, "NOTIFICACAO_NAO_ENCONTRADA", "Notificação não encontrada")
        notificacao.lida = true
        notificacao.lidaEm = Instant.now()
    }

    @Transactional
    fun marcarTodasLidas(principal: PrincipalUsuario) {
        jdbc.update("UPDATE notificacao SET lida = TRUE, lida_em = now() WHERE destinatario_id = ? AND lida = FALSE", principal.id)
    }

    @Transactional(readOnly = true)
    fun listarPreferencias(principal: PrincipalUsuario): List<PreferenciaRequest> =
        preferencias.findAllByUsuarioId(principal.id).map { PreferenciaRequest(it.tipoEvento, it.canal, it.ativo) }

    @Transactional
    fun atualizarPreferencias(
        principal: PrincipalUsuario,
        items: List<PreferenciaRequest>,
    ): List<PreferenciaRequest> =
        items.map { item ->
            val id = PreferenciaNotificacaoId(principal.id, item.tipoEvento, item.canal)
            val preference = preferencias.findById(id).orElse(PreferenciaNotificacao(principal.id, item.tipoEvento, item.canal))
            preference.ativo = item.ativo
            PreferenciaRequest(preferencias.save(preference).tipoEvento, item.canal, item.ativo)
        }

    private fun Notificacao.toResposta() = NotificacaoResposta(id, chamado.id, tipoEvento, titulo, mensagem, lida, criadoEm)
}
