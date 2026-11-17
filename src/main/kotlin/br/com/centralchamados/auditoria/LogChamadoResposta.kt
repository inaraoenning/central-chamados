package br.com.centralchamados.auditoria

import java.time.Instant
import java.util.UUID

data class LogChamadoResposta(
    val id: UUID,
    val chamadoId: UUID,
    val tipoEvento: String,
    val autorId: UUID?,
    val dados: Any,
    val criadoEm: Instant,
)
