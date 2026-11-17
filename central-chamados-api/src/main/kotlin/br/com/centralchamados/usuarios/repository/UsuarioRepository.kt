package br.com.centralchamados.usuarios.repository

import br.com.centralchamados.shared.model.Perfil
import br.com.centralchamados.usuarios.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UsuarioRepository : JpaRepository<Usuario, UUID> {
    fun findByEmailIgnoreCase(email: String): Usuario?

    fun findByPerfilAndAtivo(
        perfil: Perfil,
        ativo: Boolean = true,
    ): List<Usuario>

    @Query("select distinct u from Usuario u join u.setores s where s.id in :setorIds and u.perfil = :perfil and u.ativo = true")
    fun atendentesPorSetores(
        @Param("setorIds") setorIds: Collection<UUID>,
        @Param("perfil") perfil: Perfil,
    ): List<Usuario>
}
