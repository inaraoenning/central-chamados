package br.com.centralchamados.notificacoes.model

import br.com.centralchamados.shared.model.CanalNotificacao
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "preferencia_notificacao")
@IdClass(PreferenciaNotificacaoId::class)
class PreferenciaNotificacao(
    @Id var usuarioId: UUID = UUID.randomUUID(),
    @Id var tipoEvento: String = "",
    @Id @Enumerated(EnumType.STRING) var canal: CanalNotificacao = CanalNotificacao.EMAIL,
    var ativo: Boolean = true,
)

data class PreferenciaNotificacaoId(
    var usuarioId: UUID = UUID.randomUUID(),
    var tipoEvento: String = "",
    var canal: CanalNotificacao = CanalNotificacao.EMAIL,
) : java.io.Serializable
