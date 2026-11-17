package br.com.centralchamados.notificacoes

import br.com.centralchamados.chamados.repository.ChamadoRepository
import br.com.centralchamados.notificacoes.model.ConfiguracaoNotificacao
import br.com.centralchamados.notificacoes.repository.ConfiguracaoNotificacaoRepository
import br.com.centralchamados.shared.model.Perfil
import br.com.centralchamados.usuarios.repository.UsuarioRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class AlertasAgendados(
    private val chamados: ChamadoRepository,
    private val usuarios: UsuarioRepository,
    private val configuracoes: ConfiguracaoNotificacaoRepository,
    private val notificacoes: NotificacaoService,
) {
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun verificar() {
        val agora = Instant.now()
        val minutosPrevisao =
            configuracoes
                .findById(
                    "minutos_alerta_previsao",
                ).orElse(ConfiguracaoNotificacao("minutos_alerta_previsao", "60"))
                .valor
                .toLongOrNull()
                ?: 60
        val minutosSemAtendimento =
            configuracoes
                .findById(
                    "minutos_alerta_sem_atendimento",
                ).orElse(ConfiguracaoNotificacao("minutos_alerta_sem_atendimento", "30"))
                .valor
                .toLongOrNull()
                ?: 30
        chamados.previsoesProximas(agora, agora.plusSeconds(minutosPrevisao * 60)).forEach { chamado ->
            chamado.atendente?.let {
                notificacoes.notificar(
                    it,
                    chamado,
                    "PREVISAO_PROXIMA_VENCIMENTO",
                    "Previsão próxima do vencimento",
                    "O chamado ${chamado.numero} está próximo do vencimento.",
                    alertaAgendado = true,
                )
            }
        }
        chamados.previsoesVencidas(agora).forEach { chamado ->
            val destinatarios =
                buildSet {
                    chamado.atendente?.let(::add)
                    addAll(usuarios.findByPerfilAndAtivo(Perfil.ADMIN))
                }
            destinatarios.forEach {
                notificacoes.notificar(
                    it,
                    chamado,
                    "PREVISAO_VENCIDA",
                    "Previsão vencida",
                    "A previsão do chamado ${chamado.numero} venceu.",
                    alertaAgendado = true,
                )
            }
        }
        chamados.chamadosSemAtendimento(agora.minusSeconds(minutosSemAtendimento * 60)).forEach { chamado ->
            val destinatarios =
                buildSet {
                    addAll(usuarios.atendentesPorSetores(listOf(chamado.setor.id), Perfil.ATENDENTE))
                    addAll(usuarios.findByPerfilAndAtivo(Perfil.ADMIN))
                }
            destinatarios.forEach {
                notificacoes.notificar(
                    it,
                    chamado,
                    "CHAMADO_SEM_ATENDIMENTO",
                    "Chamado sem atendimento",
                    "O chamado ${chamado.numero} aguarda atendimento.",
                    alertaAgendado = true,
                )
            }
        }
    }
}
