package br.com.centralchamados.notificacoes

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class NotificacaoSse {
    private val conexoes = ConcurrentHashMap<UUID, MutableSet<SseEmitter>>()

    fun conectar(usuarioId: UUID): SseEmitter {
        val emitter = SseEmitter(0L)
        conexoes.computeIfAbsent(usuarioId) { ConcurrentHashMap.newKeySet() }.add(emitter)
        emitter.onCompletion { remover(usuarioId, emitter) }
        emitter.onTimeout { remover(usuarioId, emitter) }
        emitter.onError { remover(usuarioId, emitter) }
        return emitter
    }

    @TransactionalEventListener
    fun publicar(evento: NotificacaoCriada) {
        conexoes[evento.destinatarioId]?.forEach { emitter ->
            runCatching { emitter.send(SseEmitter.event().name("notificacao").data(mapOf("notificacaoId" to evento.notificacaoId))) }
                .onFailure {
                    remover(evento.destinatarioId, emitter)
                    emitter.completeWithError(it)
                }
        }
    }

    private fun remover(
        usuarioId: UUID,
        emitter: SseEmitter,
    ) {
        val emitters = conexoes[usuarioId] ?: return
        emitters.remove(emitter)
        if (emitters.isEmpty()) conexoes.remove(usuarioId, emitters)
    }
}
