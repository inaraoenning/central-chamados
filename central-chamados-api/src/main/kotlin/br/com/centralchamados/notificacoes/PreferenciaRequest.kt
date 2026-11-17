package br.com.centralchamados.notificacoes

import br.com.centralchamados.shared.model.CanalNotificacao

data class PreferenciaRequest(
    val tipoEvento: String,
    val canal: CanalNotificacao,
    val ativo: Boolean,
)
