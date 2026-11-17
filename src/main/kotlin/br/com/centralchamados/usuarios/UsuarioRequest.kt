package br.com.centralchamados.usuarios

import br.com.centralchamados.shared.model.Perfil
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

data class UsuarioRequest(
    @field:NotBlank @field:Size(max = 160) val nome: String,
    @field:Email @field:NotBlank @field:Size(max = 254) val email: String,
    @field:NotBlank @field:Size(min = 8, max = 100) val senha: String,
    @field:NotNull val perfil: Perfil?,
    val provedorId: UUID? = null,
    val setoresIds: Set<UUID> = emptySet(),
)
