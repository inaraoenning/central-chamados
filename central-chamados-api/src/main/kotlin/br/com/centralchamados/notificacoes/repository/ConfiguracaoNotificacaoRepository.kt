package br.com.centralchamados.notificacoes.repository

import br.com.centralchamados.notificacoes.model.ConfiguracaoNotificacao
import org.springframework.data.jpa.repository.JpaRepository

interface ConfiguracaoNotificacaoRepository : JpaRepository<ConfiguracaoNotificacao, String>
