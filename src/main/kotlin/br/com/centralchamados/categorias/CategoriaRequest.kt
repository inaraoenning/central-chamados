package br.com.centralchamados.categorias

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class CategoriaRequest(
    @field:NotNull val setorId: UUID?,
    @field:NotBlank @field:Size(max = 160) val nome: String,
)

data class CategoriaResposta(
    val id: UUID,
    val setorId: UUID,
    val nome: String,
    val ativo: Boolean,
)
