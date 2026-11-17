package br.com.centralchamados.notificacoes

interface PushSender {
    fun enviar(
        token: String,
        titulo: String,
        mensagem: String,
        dados: Map<String, String>,
    ): ResultadoPush
}
