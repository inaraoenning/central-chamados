package br.com.centralchamados.notificacoes

import java.time.Instant
import java.util.UUID

data class NotificacaoResposta(
    val id: UUID,
    val chamadoId: UUID,
    val tipoEvento: String,
    val titulo: String,
    val mensagem: String,
    val lida: Boolean,
    val criadoEm: Instant,
)
