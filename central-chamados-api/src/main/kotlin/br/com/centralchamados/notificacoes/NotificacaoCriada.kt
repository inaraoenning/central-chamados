package br.com.centralchamados.notificacoes

import java.util.UUID

data class NotificacaoCriada(
    val destinatarioId: UUID,
    val notificacaoId: UUID,
)
