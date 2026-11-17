package br.com.centralchamados.chamados

import jakarta.validation.constraints.NotNull
import java.time.Instant

data class AssumirChamadoRequest(
    @field:NotNull val previsaoResolucao: Instant?,
)
