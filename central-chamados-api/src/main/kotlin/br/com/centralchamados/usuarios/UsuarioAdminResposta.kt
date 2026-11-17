package br.com.centralchamados.usuarios

import br.com.centralchamados.shared.model.Perfil
import java.util.UUID

data class UsuarioAdminResposta(
    val id: UUID,
    val nome: String,
    val email: String,
    val perfil: Perfil,
    val provedorId: UUID?,
    val setoresIds: Set<UUID>,
    val ativo: Boolean,
)
