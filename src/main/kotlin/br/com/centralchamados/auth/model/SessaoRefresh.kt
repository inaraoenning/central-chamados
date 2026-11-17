package br.com.centralchamados.auth.model

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
@Table(name = "sessao_refresh")
class SessaoRefresh(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "usuario_id") var usuario: Usuario = Usuario(),
    var hash: String = "",
    @Column(name = "expira_em") var expiraEm: Instant = Instant.now(),
    @Column(name = "revogada_em") var revogadaEm: Instant? = null,
)
