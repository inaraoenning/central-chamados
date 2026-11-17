package br.com.centralchamados.chamados.model

import br.com.centralchamados.categorias.model.Categoria
import br.com.centralchamados.provedores.model.Provedor
import br.com.centralchamados.setores.model.Setor
import br.com.centralchamados.shared.model.Prioridade
import br.com.centralchamados.shared.model.StatusChamado
import br.com.centralchamados.usuarios.model.Usuario
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "chamado")
class Chamado(
    @Id var id: UUID = UUID.randomUUID(),
    var numero: String = "",
    var titulo: String = "",
    var descricao: String = "",
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "setor_id") var setor: Setor = Setor(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "categoria_id") var categoria: Categoria = Categoria(),
    @Enumerated(EnumType.STRING) var prioridade: Prioridade = Prioridade.MEDIA,
    @Enumerated(EnumType.STRING) var status: StatusChamado = StatusChamado.ABERTO,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "solicitante_id") var solicitante: Usuario = Usuario(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "provedor_id") var provedor: Provedor = Provedor(),
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "atendente_id") var atendente: Usuario? = null,
    @Column(name = "previsao_resolucao") var previsaoResolucao: Instant? = null,
    @Column(name = "observacao_resolucao") var observacaoResolucao: String? = null,
    @Column(name = "criado_em") var criadoEm: Instant = Instant.now(),
    @Column(name = "assumido_em") var assumidoEm: Instant? = null,
    @Column(name = "concluido_em") var concluidoEm: Instant? = null,
    @Column(name = "atualizado_em") var atualizadoEm: Instant = Instant.now(),
    @Version var version: Long = 0,
)
