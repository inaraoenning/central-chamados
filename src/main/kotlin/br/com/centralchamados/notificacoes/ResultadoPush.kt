package br.com.centralchamados.notificacoes

data class ResultadoPush(
    val aceito: Boolean,
    val dispositivoInvalido: Boolean = false,
)
