package br.com.centralchamados.notificacoes

import br.com.centralchamados.notificacoes.repository.NotificacaoEnvioRepository
import br.com.centralchamados.shared.model.StatusEnvio
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OutboxWorker(
    private val envios: NotificacaoEnvioRepository,
    private val processamento: NotificacaoEnvioService,
) {
    @Scheduled(fixedDelayString = "\${app.outbox.interval-ms:5000}")
    fun processar() {
        envios
            .pendentes(StatusEnvio.PENDENTE, 3, Instant.now(), PageRequest.of(0, 100))
            .forEach { processamento.processar(it.id) }
    }
}
