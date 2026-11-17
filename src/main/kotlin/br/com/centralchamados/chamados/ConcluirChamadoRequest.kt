package br.com.centralchamados.chamados

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ConcluirChamadoRequest(
    @field:NotBlank @field:Size(min = 10, max = 5000) val observacaoResolucao: String,
)
