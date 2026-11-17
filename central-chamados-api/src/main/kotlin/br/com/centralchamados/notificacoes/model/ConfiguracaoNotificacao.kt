package br.com.centralchamados.notificacoes.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "configuracao_notificacao")
class ConfiguracaoNotificacao(
    @Id var chave: String = "",
    var valor: String = "",
)
