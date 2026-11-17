package br.com.centralchamados.notificacoes

interface EmailSender {
    fun enviar(
        destinatario: String,
        titulo: String,
        mensagem: String,
    )
}
