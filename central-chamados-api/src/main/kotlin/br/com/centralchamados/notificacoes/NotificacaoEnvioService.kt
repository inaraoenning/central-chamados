package br.com.centralchamados.notificacoes

import br.com.centralchamados.notificacoes.repository.DispositivoPushRepository
import br.com.centralchamados.notificacoes.repository.NotificacaoEnvioRepository
import br.com.centralchamados.shared.model.CanalNotificacao
import br.com.centralchamados.shared.model.StatusEnvio
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class NotificacaoEnvioService(
    private val envios: NotificacaoEnvioRepository,
    private val push: PushSender,
    private val email: EmailSender,
    private val dispositivos: DispositivoPushRepository,
) {
    @Transactional
    fun processar(id: UUID) {
        val envio = envios.findById(id).orElse(null) ?: return
        if (envio.status != StatusEnvio.PENDENTE) return
        try {
            when (envio.canal) {
                CanalNotificacao.EMAIL ->
                    email.enviar(
                        envio.notificacao.destinatario.email,
                        envio.notificacao.titulo,
                        envio.notificacao.mensagem,
                    )
                CanalNotificacao.PUSH ->
                    dispositivos
                        .findByUsuarioIdAndAtivoTrue(envio.notificacao.destinatario.id)
                        .forEach { dispositivo ->
                            val resultado =
                                push.enviar(
                                    dispositivo.token,
                                    envio.notificacao.titulo,
                                    envio.notificacao.mensagem,
                                    mapOf(
                                        "chamadoId" to
                                            envio.notificacao.chamado.id
                                                .toString(),
                                        "tipoEvento" to envio.notificacao.tipoEvento,
                                    ),
                                )
                            if (resultado.dispositivoInvalido) dispositivo.ativo = false
                        }
            }
            envio.status = StatusEnvio.ENVIADO
            envio.enviadoEm = Instant.now()
            envio.proximaTentativaEm = null
            envio.erro = null
        } catch (e: Exception) {
            envio.tentativas += 1
            envio.erro = e.message?.take(1000) ?: "Falha de envio"
            envio.status = if (envio.tentativas >= 3) StatusEnvio.FALHOU else StatusEnvio.PENDENTE
            envio.proximaTentativaEm =
                if (envio.status == StatusEnvio.PENDENTE) {
                    Instant.now().plusSeconds(10L * (1L shl (envio.tentativas - 1)))
                } else {
                    null
                }
        }
    }
}
