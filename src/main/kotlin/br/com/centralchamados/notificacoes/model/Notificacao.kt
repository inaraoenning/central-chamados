package br.com.centralchamados.notificacoes.model

import br.com.centralchamados.chamados.model.Chamado
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
@Table(name = "notificacao")
class Notificacao(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "destinatario_id") var destinatario: Usuario = Usuario(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chamado_id") var chamado: Chamado = Chamado(),
    @Column(name = "tipo_evento") var tipoEvento: String = "",
    var titulo: String = "",
    var mensagem: String = "",
    var lida: Boolean = false,
    @Column(name = "lida_em") var lidaEm: Instant? = null,
    @Column(name = "criado_em") var criadoEm: Instant = Instant.now(),
    @Column(name = "alerta_agendado") var alertaAgendado: Boolean = false,
)
