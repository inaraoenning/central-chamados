package br.com.centralchamados.chamados

import br.com.centralchamados.shared.model.Prioridade
import br.com.centralchamados.shared.model.StatusChamado
import java.time.Instant
import java.util.UUID

data class ChamadoResposta(
    val id: UUID,
    val numero: String,
    val titulo: String,
    val descricao: String,
    val setorId: UUID,
    val setor: String,
    val categoriaId: UUID,
    val categoria: String,
    val prioridade: Prioridade,
    val status: StatusChamado,
    val solicitanteId: UUID,
    val solicitante: String,
    val provedorId: UUID,
    val atendenteId: UUID?,
    val atendente: String?,
    val previsaoResolucao: Instant?,
    val observacaoResolucao: String?,
    val criadoEm: Instant,
    val assumidoEm: Instant?,
    val concluidoEm: Instant?,
    val atualizadoEm: Instant,
    val version: Long,
)
