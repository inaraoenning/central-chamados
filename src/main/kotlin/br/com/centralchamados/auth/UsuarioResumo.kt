package br.com.centralchamados.auth

import br.com.centralchamados.shared.model.Perfil
import java.util.UUID

data class UsuarioResumo(
    val id: UUID,
    val nome: String,
    val email: String,
    val perfil: Perfil,
    val provedorId: UUID?,
)
