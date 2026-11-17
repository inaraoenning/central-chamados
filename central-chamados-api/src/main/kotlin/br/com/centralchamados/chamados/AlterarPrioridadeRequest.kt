package br.com.centralchamados.chamados

import br.com.centralchamados.shared.model.Prioridade
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AlterarPrioridadeRequest(
    @field:NotNull val prioridade: Prioridade?,
    @field:Size(max = 500) val motivo: String? = null,
)
