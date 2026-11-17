package br.com.centralchamados.notificacoes

import jakarta.validation.constraints.NotBlank

data class ConfiguracaoRequest(
    @field:NotBlank val chave: String,
    @field:NotBlank val valor: String,
)
