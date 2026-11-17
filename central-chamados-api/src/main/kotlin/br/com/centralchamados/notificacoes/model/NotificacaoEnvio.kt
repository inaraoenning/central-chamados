package br.com.centralchamados.notificacoes.model

import br.com.centralchamados.shared.model.CanalNotificacao
import br.com.centralchamados.shared.model.StatusEnvio
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notificacao_envio")
class NotificacaoEnvio(
    @Id var id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "notificacao_id") var notificacao: Notificacao = Notificacao(),
    @Enumerated(EnumType.STRING) var canal: CanalNotificacao = CanalNotificacao.EMAIL,
    @Enumerated(EnumType.STRING) var status: StatusEnvio = StatusEnvio.PENDENTE,
    var tentativas: Int = 0,
    var erro: String? = null,
    @Column(name = "enviado_em") var enviadoEm: Instant? = null,
    @Column(name = "proxima_tentativa_em") var proximaTentativaEm: Instant? = null,
)
