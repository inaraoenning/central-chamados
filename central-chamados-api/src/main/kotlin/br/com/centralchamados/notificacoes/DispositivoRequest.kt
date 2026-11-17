package br.com.centralchamados.notificacoes

import jakarta.validation.constraints.NotBlank

data class DispositivoRequest(
    @field:NotBlank val token: String,
    @field:NotBlank val plataforma: String,
)
