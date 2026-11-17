package br.com.centralchamados.setores

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SetorRequest(
    @field:NotBlank @field:Size(max = 160) val nome: String,
    @field:Size(max = 500) val descricao: String?,
)

data class SetorResposta(
    val id: java.util.UUID,
    val nome: String,
    val descricao: String?,
    val ativo: Boolean,
)
