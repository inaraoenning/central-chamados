package br.com.centralchamados.chamados

import br.com.centralchamados.shared.model.Prioridade
import br.com.centralchamados.shared.model.StatusChamado
import java.time.Instant
import java.util.UUID

data class ChamadoFiltros(
    val status: StatusChamado? = null,
    val prioridade: Prioridade? = null,
    val setorId: UUID? = null,
    val categoriaId: UUID? = null,
    val provedorId: UUID? = null,
    val atendenteId: UUID? = null,
    val de: Instant? = null,
    val ate: Instant? = null,
    val busca: String? = null,
)
