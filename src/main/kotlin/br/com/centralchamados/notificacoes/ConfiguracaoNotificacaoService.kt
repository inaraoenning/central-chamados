package br.com.centralchamados.notificacoes

import br.com.centralchamados.notificacoes.model.ConfiguracaoNotificacao
import br.com.centralchamados.notificacoes.repository.ConfiguracaoNotificacaoRepository
import br.com.centralchamados.shared.api.ErroApi
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConfiguracaoNotificacaoService(
    private val configuracoes: ConfiguracaoNotificacaoRepository,
) {
    fun listar(): Map<String, String> = configuracoes.findAll().associate { it.chave to it.valor }

    @Transactional
    fun atualizar(items: List<ConfiguracaoRequest>): Map<String, String> {
        items.forEach { item ->
            if (item.chave !in setOf("minutos_alerta_previsao", "minutos_alerta_sem_atendimento")) {
                throw ErroApi(HttpStatus.BAD_REQUEST, "CONFIGURACAO_INVALIDA", "Chave de configuração desconhecida")
            }
            if (item.valor.toLongOrNull()?.let { it >= 0 } != true) {
                throw ErroApi(HttpStatus.BAD_REQUEST, "CONFIGURACAO_INVALIDA", "Valor deve ser um inteiro não negativo")
            }
            configuracoes.save(ConfiguracaoNotificacao(item.chave, item.valor))
        }
        return listar()
    }
}
