package br.com.centralchamados.notificacoes

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class SmtpEmailSender(
    private val mailSender: JavaMailSender,
    @Value("\${SMTP_FROM:no-reply@central-chamados.local}") private val remetente: String,
) : EmailSender {
    override fun enviar(
        destinatario: String,
        titulo: String,
        mensagem: String,
    ) {
        val email = SimpleMailMessage()
        email.from = remetente
        email.setTo(destinatario)
        email.subject = titulo
        email.text = mensagem
        mailSender.send(email)
    }
}
