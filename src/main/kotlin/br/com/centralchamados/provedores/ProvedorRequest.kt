package br.com.centralchamados.provedores

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class ProvedorRequest(
    @field:NotBlank @field:Size(max = 160) val nome: String,
    @field:Size(min = 14, max = 14) val cnpj: String?,
)

data class ProvedorResposta(
    val id: UUID,
    val nome: String,
    val cnpj: String?,
    val ativo: Boolean,
    val criadoEm: Instant,
)
