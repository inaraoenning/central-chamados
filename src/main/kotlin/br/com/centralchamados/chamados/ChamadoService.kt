package br.com.centralchamados.chamados

import br.com.centralchamados.auditoria.AuditoriaService
import br.com.centralchamados.auth.PrincipalUsuario
import br.com.centralchamados.categorias.model.Categoria
import br.com.centralchamados.categorias.repository.CategoriaRepository
import br.com.centralchamados.chamados.model.Chamado
import br.com.centralchamados.chamados.repository.ChamadoRepository
import br.com.centralchamados.chamados.repository.acessoDoPerfil
import br.com.centralchamados.notificacoes.NotificacaoService
import br.com.centralchamados.provedores.model.Provedor
import br.com.centralchamados.provedores.repository.ProvedorRepository
import br.com.centralchamados.setores.model.Setor
import br.com.centralchamados.setores.repository.SetorRepository
import br.com.centralchamados.shared.api.ErroApi
import br.com.centralchamados.shared.model.Perfil
import br.com.centralchamados.shared.model.Prioridade
import br.com.centralchamados.shared.model.StatusChamado
import br.com.centralchamados.usuarios.model.Usuario
import br.com.centralchamados.usuarios.repository.UsuarioRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.Year
import java.util.UUID

@Service
class ChamadoService(
    private val chamados: ChamadoRepository,
    private val setores: SetorRepository,
    private val categorias: CategoriaRepository,
    private val provedores: ProvedorRepository,
    private val usuarios: UsuarioRepository,
    private val notificacoes: NotificacaoService,
    private val auditoria: AuditoriaService,
    private val jdbc: JdbcTemplate,
) {
    @Transactional
    fun abrir(
        principal: PrincipalUsuario,
        body: AbrirChamadoRequest,
    ): ChamadoResposta {
        if (principal.perfil != Perfil.SOLICITANTE) negar()
        val setor =
            setores
                .findById(
                    body.setorId!!,
                ).orElseThrow { ErroApi(HttpStatus.NOT_FOUND, "SETOR_NAO_ENCONTRADO", "Setor não encontrado") }
        val categoria =
            categorias.findById(body.categoriaId!!).orElseThrow {
                ErroApi(HttpStatus.NOT_FOUND, "CATEGORIA_NAO_ENCONTRADA", "Categoria não encontrada")
            }
        if (!setor.ativo || !categoria.ativo || categoria.setor.id != setor.id) {
            throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "CATEGORIA_SETOR_INVALIDOS", "A categoria não pertence ao setor informado")
        }
        val usuario = usuarios.findById(principal.id).orElseThrow { naoEncontrado() }
        val provedor =
            usuario.provedor ?: throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "PROVEDOR_OBRIGATORIO", "Solicitante sem provedor vinculado")
        val agora = Instant.now()
        val sequencial = jdbc.queryForObject("SELECT nextval('chamado_seq')", Long::class.javaObjectType)!!
        val chamado =
            chamados.saveAndFlush(
                Chamado(
                    numero = "CH-${Year.now(java.time.ZoneOffset.UTC)}-${sequencial.toString().padStart(6, '0')}",
                    titulo = body.titulo.trim(),
                    descricao = body.descricao.trim(),
                    setor = setor,
                    categoria = categoria,
                    prioridade = body.prioridade!!,
                    status = StatusChamado.ABERTO,
                    solicitante = usuario,
                    provedor = provedor,
                    criadoEm = agora,
                    atualizadoEm = agora,
                ),
            )
        auditoria.registrar(chamado.id, "CRIADO", principal.id, mapOf("provedorId" to provedor.id, "setorId" to setor.id))
        val atendentes = usuarios.atendentesPorSetores(listOf(setor.id), Perfil.ATENDENTE)
        atendentes.forEach {
            notificacoes.notificar(
                it,
                chamado,
                "CHAMADO_CRIADO",
                "Novo chamado ${chamado.numero}",
                "Um novo chamado foi aberto no setor ${setor.nome}.",
                principal.id,
            )
        }
        if (chamado.prioridade == Prioridade.CRITICA) {
            (atendentes + usuarios.findByPerfilAndAtivo(Perfil.ADMIN)).distinctBy { it.id }.forEach {
                notificacoes.notificar(
                    it,
                    chamado,
                    "CHAMADO_CRITICO",
                    "Chamado crítico ${chamado.numero}",
                    "Um chamado crítico foi aberto.",
                    principal.id,
                )
            }
        }
        return chamado.toResposta()
    }

    @Transactional(readOnly = true)
    fun listar(
        principal: PrincipalUsuario,
        filtros: ChamadoFiltros,
        pageable: Pageable,
    ): Page<ChamadoResposta> = chamados.findAll(filtrar(principal, filtros), pageable).map { it.toResposta() }

    @Transactional(readOnly = true)
    fun meus(
        principal: PrincipalUsuario,
        pageable: Pageable,
    ): Page<ChamadoResposta> {
        if (principal.perfil != Perfil.SOLICITANTE) negar()
        return chamados.meus(principal.id, pageable).map { it.toResposta() }
    }

    @Transactional(readOnly = true)
    fun fila(
        principal: PrincipalUsuario,
        pageable: Pageable,
    ): Page<ChamadoResposta> {
        if (principal.perfil != Perfil.ATENDENTE) negar()
        if (principal.setores.isEmpty()) return Page.empty(pageable)
        val ordenacao = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.unsorted())
        return chamados.filaOrdenada(principal.setores, ordenacao).map { it.toResposta() }
    }

    @Transactional(readOnly = true)
    fun obter(
        principal: PrincipalUsuario,
        id: UUID,
    ): ChamadoResposta = visivel(principal, id).toResposta()

    @Transactional
    fun assumir(
        principal: PrincipalUsuario,
        id: UUID,
        body: AssumirChamadoRequest,
    ): ChamadoResposta {
        val chamado = visivel(principal, id)
        exigirEquipe(principal, chamado)
        if (chamado.status == StatusChamado.EM_ANDAMENTO) {
            throw ErroApi(HttpStatus.CONFLICT, "CHAMADO_JA_ASSUMIDO", "O chamado já foi assumido por outra pessoa")
        }
        if (chamado.status != StatusChamado.ABERTO) transicaoInvalida()
        val previsao =
            body.previsaoResolucao
                ?: throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "PREVISAO_OBRIGATORIA", "A previsão é obrigatória ao assumir")
        validarPrevisao(previsao)
        val anterior = chamado.status
        val atendente = usuarios.findById(principal.id).orElseThrow { naoEncontrado() }
        chamado.atendente = atendente
        chamado.assumidoEm = Instant.now()
        chamado.previsaoResolucao = previsao
        chamado.status = StatusChamado.EM_ANDAMENTO
        chamado.atualizadoEm = Instant.now()
        auditoria.registrar(chamado.id, "ASSUMIDO", principal.id, mapOf("atendenteId" to principal.id, "previsaoResolucao" to previsao))
        auditoria.registrar(chamado.id, "STATUS_ALTERADO", principal.id, mapOf("antes" to anterior, "depois" to chamado.status))
        notificacoes.notificar(
            chamado.solicitante,
            chamado,
            "CHAMADO_ASSUMIDO",
            "Chamado assumido",
            "O atendente ${atendente.nome} assumiu ${chamado.numero}. Previsão: $previsao.",
            principal.id,
        )
        return chamados.saveAndFlush(chamado).toResposta()
    }

    @Transactional
    fun alterarPrioridade(
        principal: PrincipalUsuario,
        id: UUID,
        body: AlterarPrioridadeRequest,
    ): ChamadoResposta {
        val chamado = visivel(principal, id)
        exigirEquipe(principal, chamado)
        if (chamado.status == StatusChamado.CONCLUIDO) transicaoInvalida()
        val anterior = chamado.prioridade
        val nova = body.prioridade!!
        if (anterior != nova) {
            chamado.prioridade = nova
            chamado.atualizadoEm = Instant.now()
            auditoria.registrar(
                chamado.id,
                "PRIORIDADE_ALTERADA",
                principal.id,
                mapOf("antes" to anterior, "depois" to nova, "motivo" to body.motivo),
            )
            notificacoes.notificar(
                chamado.solicitante,
                chamado,
                "PRIORIDADE_ALTERADA",
                "Prioridade alterada",
                "A prioridade de ${chamado.numero} foi alterada para $nova.",
                principal.id,
            )
            if (nova == Prioridade.CRITICA) {
                (usuarios.atendentesPorSetores(listOf(chamado.setor.id), Perfil.ATENDENTE) + usuarios.findByPerfilAndAtivo(Perfil.ADMIN))
                    .distinctBy { it.id }
                    .forEach {
                        notificacoes.notificar(
                            it,
                            chamado,
                            "CHAMADO_CRITICO",
                            "Chamado crítico ${chamado.numero}",
                            "Um chamado foi reclassificado como crítico.",
                            principal.id,
                        )
                    }
            }
        }
        return chamados.saveAndFlush(chamado).toResposta()
    }

    @Transactional
    fun alterarPrevisao(
        principal: PrincipalUsuario,
        id: UUID,
        body: AlterarPrevisaoRequest,
    ): ChamadoResposta {
        val chamado = visivel(principal, id)
        exigirResponsavelOuAdmin(principal, chamado)
        if (chamado.status != StatusChamado.EM_ANDAMENTO) transicaoInvalida()
        val nova =
            body.previsaoResolucao ?: throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "PREVISAO_OBRIGATORIA", "A previsão é obrigatória")
        validarPrevisao(nova)
        val anterior = chamado.previsaoResolucao
        chamado.previsaoResolucao = nova
        chamado.atualizadoEm = Instant.now()
        auditoria.registrar(chamado.id, "PREVISAO_ALTERADA", principal.id, mapOf("antes" to anterior, "depois" to nova))
        notificacoes.notificar(
            chamado.solicitante,
            chamado,
            "PREVISAO_ALTERADA",
            "Previsão alterada",
            "A previsão de ${chamado.numero} foi atualizada para $nova.",
            principal.id,
        )
        return chamados.saveAndFlush(chamado).toResposta()
    }

    @Transactional
    fun concluir(
        principal: PrincipalUsuario,
        id: UUID,
        body: ConcluirChamadoRequest,
    ): ChamadoResposta {
        val chamado = visivel(principal, id)
        exigirResponsavelOuAdmin(principal, chamado)
        if (chamado.status != StatusChamado.EM_ANDAMENTO) transicaoInvalida()
        if (chamado.atendente ==
            null
        ) {
            throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "ATENDENTE_OBRIGATORIO", "Chamado sem atendente atribuído")
        }
        val observacao = body.observacaoResolucao.trim()
        if (observacao.length < 10) {
            throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "OBSERVACAO_OBRIGATORIA", "A observação deve conter ao menos 10 caracteres")
        }
        val anterior = chamado.status
        val agora = Instant.now()
        chamado.status = StatusChamado.CONCLUIDO
        chamado.observacaoResolucao = observacao
        chamado.concluidoEm = agora
        chamado.atualizadoEm = agora
        auditoria.registrar(chamado.id, "CONCLUIDO", principal.id, mapOf("observacaoResolucao" to observacao))
        auditoria.registrar(chamado.id, "STATUS_ALTERADO", principal.id, mapOf("antes" to anterior, "depois" to chamado.status))
        notificacoes.notificar(
            chamado.solicitante,
            chamado,
            "CHAMADO_CONCLUIDO",
            "Chamado concluído",
            "O chamado ${chamado.numero} foi concluído: $observacao",
            principal.id,
        )
        return chamados.saveAndFlush(chamado).toResposta()
    }

    private fun filtrar(
        principal: PrincipalUsuario,
        filtros: ChamadoFiltros,
    ): Specification<Chamado> {
        var spec = acessoDoPerfil(principal.id, principal.perfil, principal.setores)

        fun adicionar(part: Specification<Chamado>) {
            spec = spec.and(part)
        }
        filtros.status?.let { valor -> adicionar(Specification { root, _, cb -> cb.equal(root.get<StatusChamado>("status"), valor) }) }
        filtros.prioridade?.let { valor -> adicionar(Specification { root, _, cb -> cb.equal(root.get<Prioridade>("prioridade"), valor) }) }
        filtros.setorId?.let { valor ->
            adicionar(Specification { root, _, cb -> cb.equal(root.get<Setor>("setor").get<UUID>("id"), valor) })
        }
        filtros.categoriaId?.let { valor ->
            adicionar(Specification { root, _, cb -> cb.equal(root.get<Categoria>("categoria").get<UUID>("id"), valor) })
        }
        filtros.provedorId?.let { valor ->
            adicionar(Specification { root, _, cb -> cb.equal(root.get<Provedor>("provedor").get<UUID>("id"), valor) })
        }
        filtros.atendenteId?.let { valor ->
            adicionar(Specification { root, _, cb -> cb.equal(root.get<Usuario>("atendente").get<UUID>("id"), valor) })
        }
        filtros.de?.let { valor -> adicionar(Specification { root, _, cb -> cb.greaterThanOrEqualTo(root.get("criadoEm"), valor) }) }
        filtros.ate?.let { valor -> adicionar(Specification { root, _, cb -> cb.lessThanOrEqualTo(root.get("criadoEm"), valor) }) }
        filtros.busca?.takeIf { it.isNotBlank() }?.let { termo ->
            val padrao = "%${termo.lowercase()}%"
            adicionar(
                Specification { root, _, cb ->
                    cb.or(
                        cb.like(cb.lower(root.get("titulo")), padrao),
                        cb.like(cb.lower(root.get("descricao")), padrao),
                        cb.like(cb.lower(root.get("numero")), padrao),
                    )
                },
            )
        }
        return spec
    }

    private fun visivel(
        principal: PrincipalUsuario,
        id: UUID,
    ): Chamado {
        val spec =
            Specification<Chamado> { root, query, cb ->
                val escopo = acessoDoPerfil(principal.id, principal.perfil, principal.setores).toPredicate(root, query, cb)
                cb.and(escopo ?: cb.conjunction(), cb.equal(root.get<UUID>("id"), id))
            }
        return chamados.findOne(spec).orElseThrow { ErroApi(HttpStatus.NOT_FOUND, "CHAMADO_NAO_ENCONTRADO", "Chamado não encontrado") }
    }

    private fun exigirEquipe(
        principal: PrincipalUsuario,
        chamado: Chamado,
    ) {
        if (principal.perfil != Perfil.ADMIN && (principal.perfil != Perfil.ATENDENTE || chamado.setor.id !in principal.setores)) negar()
    }

    private fun exigirResponsavelOuAdmin(
        principal: PrincipalUsuario,
        chamado: Chamado,
    ) {
        if (principal.perfil != Perfil.ADMIN && (principal.perfil != Perfil.ATENDENTE || chamado.atendente?.id != principal.id)) negar()
    }

    private fun validarPrevisao(previsao: Instant) {
        if (!previsao.isAfter(
                Instant.now(),
            )
        ) {
            throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "PREVISAO_INVALIDA", "A previsão deve ser uma data futura")
        }
    }

    private fun Chamado.toResposta() =
        ChamadoResposta(
            id,
            numero,
            titulo,
            descricao,
            setor.id,
            setor.nome,
            categoria.id,
            categoria.nome,
            prioridade,
            status,
            solicitante.id,
            solicitante.nome,
            provedor.id,
            atendente?.id,
            atendente?.nome,
            previsaoResolucao,
            observacaoResolucao,
            criadoEm,
            assumidoEm,
            concluidoEm,
            atualizadoEm,
            version,
        )

    private fun naoEncontrado() = ErroApi(HttpStatus.NOT_FOUND, "CHAMADO_NAO_ENCONTRADO", "Chamado não encontrado")

    private fun negar(): Nothing = throw ErroApi(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Acesso negado")

    private fun transicaoInvalida(): Nothing =
        throw ErroApi(HttpStatus.UNPROCESSABLE_ENTITY, "TRANSICAO_INVALIDA", "Transição de estado inválida")
}
