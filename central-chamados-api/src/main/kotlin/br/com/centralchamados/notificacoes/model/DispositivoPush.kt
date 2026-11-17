package br.com.centralchamados.notificacoes.model

import br.com.centralchamados.usuarios.model.Usuario
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "dispositivo_push")
class DispositivoPush(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id") var usuario: Usuario = Usuario(),
    var token: String = "",
    var plataforma: String = "",
    var ativo: Boolean = true,
    @Column(name = "criado_em") var criadoEm: Instant = Instant.now(),
)
