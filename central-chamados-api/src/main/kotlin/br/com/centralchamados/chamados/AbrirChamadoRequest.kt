package br.com.centralchamados.chamados

import br.com.centralchamados.shared.model.Prioridade
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class AbrirChamadoRequest(
    @field:NotNull val setorId: UUID?,
    @field:NotNull val categoriaId: UUID?,
    @field:NotNull val prioridade: Prioridade?,
    @field:NotBlank @field:Size(max = 200) val titulo: String,
    @field:NotBlank @field:Size(max = 5000) val descricao: String,
)
