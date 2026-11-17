package br.com.centralchamados.usuarios.model

import br.com.centralchamados.provedores.model.Provedor
import br.com.centralchamados.setores.model.Setor
import br.com.centralchamados.shared.model.Perfil
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "usuario")
class Usuario(
    @Id var id: UUID = UUID.randomUUID(),
    var nome: String = "",
    var email: String = "",
    @Column(name = "senha_hash") var senhaHash: String = "",
    @Enumerated(EnumType.STRING) var perfil: Perfil = Perfil.SOLICITANTE,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provedor_id") var provedor: Provedor? = null,
    var ativo: Boolean = true,
    @Column(name = "criado_em") var criadoEm: Instant = Instant.now(),
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "usuario_setor",
        joinColumns = [JoinColumn(name = "usuario_id")],
        inverseJoinColumns = [JoinColumn(name = "setor_id")],
    )
    var setores: MutableSet<Setor> = mutableSetOf(),
)
