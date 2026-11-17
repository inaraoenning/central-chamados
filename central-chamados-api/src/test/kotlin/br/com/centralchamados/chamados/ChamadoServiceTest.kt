package br.com.centralchamados.chamados

import br.com.centralchamados.auditoria.AuditoriaService
import br.com.centralchamados.auth.PrincipalUsuario
import br.com.centralchamados.categorias.model.Categoria
import br.com.centralchamados.categorias.repository.CategoriaRepository
import br.com.centralchamados.chamados.model.Chamado
import br.com.centralchamados.chamados.repository.ChamadoRepository
import br.com.centralchamados.notificacoes.NotificacaoService
import br.com.centralchamados.notificacoes.repository.NotificacaoRepository
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.Optional
import java.util.UUID

class ChamadoServiceTest {
    private val chamados = mockk<ChamadoRepository>(relaxed = true)
    private val setores = mockk<SetorRepository>()
    private val categorias = mockk<CategoriaRepository>()
    private val provedores = mockk<ProvedorRepository>()
    private val usuarios = mockk<UsuarioRepository>()
    private val notificacoes = mockk<NotificacaoService>(relaxed = true)
    private val auditoria = mockk<AuditoriaService>(relaxed = true)
    private val jdbc = mockk<JdbcTemplate>(relaxed = true)
    private val service = ChamadoService(chamados, setores, categorias, provedores, usuarios, notificacoes, auditoria, jdbc)

    @Test
    fun `nao permite categoria de outro setor`() {
        val solicitante = usuario(Perfil.SOLICITANTE)
        val setorA = Setor(nome = "A")
        val setorB = Setor(nome = "B")
        val categoria = Categoria(setor = setorB)
        every { setores.findById(setorA.id) } returns Optional.of(setorA)
        every { categorias.findById(categoria.id) } returns Optional.of(categoria)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.abrir(
                    principal(solicitante, Perfil.SOLICITANTE),
                    AbrirChamadoRequest(setorA.id, categoria.id, Prioridade.MEDIA, "Título", "Descrição"),
                )
            }

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, erro.status)
        assertEquals("CATEGORIA_SETOR_INVALIDOS", erro.codigo)
    }

    @Test
    fun `rejeita transicao de chamado concluido`() {
        val setor = Setor(nome = "Setor")
        val atendente = usuario(Perfil.ATENDENTE, setor)
        val chamado = chamado(setor, atendente, StatusChamado.CONCLUIDO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.assumir(
                    principal(atendente, Perfil.ATENDENTE, setor),
                    chamado.id,
                    AssumirChamadoRequest(Instant.now().plusSeconds(3600)),
                )
            }

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, erro.status)
        assertEquals("TRANSICAO_INVALIDA", erro.codigo)
    }

    @Test
    fun `rejeita conclusao sem observacao minima`() {
        val setor = Setor(nome = "Setor")
        val atendente = usuario(Perfil.ATENDENTE, setor)
        val chamado = chamado(setor, atendente, StatusChamado.EM_ANDAMENTO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.concluir(principal(atendente, Perfil.ATENDENTE, setor), chamado.id, ConcluirChamadoRequest("curto"))
            }

        assertEquals("OBSERVACAO_OBRIGATORIA", erro.codigo)
    }

    @Test
    fun `rejeita conclusao sem atendente atribuido`() {
        val setor = Setor(nome = "Setor")
        val admin = usuario(Perfil.ADMIN)
        val chamado = chamado(setor, null, StatusChamado.EM_ANDAMENTO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.concluir(principal(admin, Perfil.ADMIN), chamado.id, ConcluirChamadoRequest("Resolução válida para o chamado"))
            }

        assertEquals("ATENDENTE_OBRIGATORIO", erro.codigo)
    }

    @Test
    fun `autor nao recebe notificacao da propria acao`() {
        val repository = mockk<NotificacaoRepository>(relaxed = true)
        val serviceNotificacao =
            NotificacaoService(
                repository,
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
        val autor = usuario(Perfil.ATENDENTE)
        val chamado = chamado(Setor(nome = "Setor"), autor, StatusChamado.ABERTO)

        val criada = serviceNotificacao.notificar(autor, chamado, "CHAMADO_ALTERADO", "Título", "Mensagem", autor.id)

        assertFalse(criada)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `assumir registra atendente e transiciona para andamento`() {
        val setor = Setor(nome = "Setor")
        val atendente = usuario(Perfil.ATENDENTE, setor)
        val chamado = chamado(setor, null, StatusChamado.ABERTO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)
        every { usuarios.findById(atendente.id) } returns Optional.of(atendente)
        every { chamados.saveAndFlush(chamado) } returns chamado
        every { jdbc.update(any<String>(), *anyVararg()) } returns 1
        every { notificacoes.notificar(any(), any(), any(), any(), any(), any(), any()) } returns true

        val previsao = Instant.now().plusSeconds(3600)
        val resposta =
            service.assumir(
                principal(atendente, Perfil.ATENDENTE, setor),
                chamado.id,
                AssumirChamadoRequest(previsao),
            )

        assertEquals(StatusChamado.EM_ANDAMENTO, resposta.status)
        assertEquals(atendente.id, resposta.atendenteId)
        assertEquals(previsao, resposta.previsaoResolucao)
    }

    @Test
    fun `nao permite concluir chamado aberto`() {
        val admin = usuario(Perfil.ADMIN)
        val chamado = chamado(Setor(nome = "Setor"), null, StatusChamado.ABERTO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.concluir(principal(admin, Perfil.ADMIN), chamado.id, ConcluirChamadoRequest("Resolução longa e válida"))
            }

        assertEquals("TRANSICAO_INVALIDA", erro.codigo)
    }

    @Test
    fun `rejeita previsao de resolucao no passado`() {
        val setor = Setor(nome = "Setor")
        val atendente = usuario(Perfil.ATENDENTE, setor)
        val chamado = chamado(setor, null, StatusChamado.ABERTO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.assumir(
                    principal(atendente, Perfil.ATENDENTE, setor),
                    chamado.id,
                    AssumirChamadoRequest(Instant.now().minusSeconds(60)),
                )
            }

        assertEquals("PREVISAO_INVALIDA", erro.codigo)
    }

    @Test
    fun `nao permite alterar previsao em chamado aberto`() {
        val admin = usuario(Perfil.ADMIN)
        val chamado = chamado(Setor(nome = "Setor"), null, StatusChamado.ABERTO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.alterarPrevisao(
                    principal(admin, Perfil.ADMIN),
                    chamado.id,
                    AlterarPrevisaoRequest(Instant.now().plusSeconds(3600)),
                )
            }

        assertEquals("TRANSICAO_INVALIDA", erro.codigo)
    }

    @Test
    fun `nao permite alterar prioridade de chamado concluido`() {
        val admin = usuario(Perfil.ADMIN)
        val chamado = chamado(Setor(nome = "Setor"), admin, StatusChamado.CONCLUIDO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.alterarPrioridade(
                    principal(admin, Perfil.ADMIN),
                    chamado.id,
                    AlterarPrioridadeRequest(Prioridade.ALTA),
                )
            }

        assertEquals("TRANSICAO_INVALIDA", erro.codigo)
    }

    @Test
    fun `atendente do setor nao pode concluir chamado atribuido a outro`() {
        val setor = Setor(nome = "Setor")
        val responsavel = usuario(Perfil.ATENDENTE, setor)
        val outroAtendente = usuario(Perfil.ATENDENTE, setor)
        val chamado = chamado(setor, responsavel, StatusChamado.EM_ANDAMENTO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.concluir(
                    principal(outroAtendente, Perfil.ATENDENTE, setor),
                    chamado.id,
                    ConcluirChamadoRequest("Resolução longa e válida"),
                )
            }

        assertEquals("ACESSO_NEGADO", erro.codigo)
    }

    @Test
    fun `solicitante nao pode assumir chamado`() {
        val setor = Setor(nome = "Setor")
        val solicitante = usuario(Perfil.SOLICITANTE, setor)
        val chamado = chamado(setor, null, StatusChamado.ABERTO)
        every { chamados.findOne(any<Specification<Chamado>>()) } returns Optional.of(chamado)

        val erro =
            assertThrows(ErroApi::class.java) {
                service.assumir(
                    principal(solicitante, Perfil.SOLICITANTE),
                    chamado.id,
                    AssumirChamadoRequest(Instant.now().plusSeconds(3600)),
                )
            }

        assertEquals("ACESSO_NEGADO", erro.codigo)
    }

    private fun usuario(
        perfil: Perfil,
        setor: Setor? = null,
    ) = Usuario(
        nome = "Pessoa",
        email = "${UUID.randomUUID()}@example.test",
        perfil = perfil,
        setores =
            setor?.let { mutableSetOf(it) } ?: mutableSetOf(),
        provedor = if (perfil == Perfil.SOLICITANTE) Provedor(nome = "P") else null,
    )

    private fun principal(
        usuario: Usuario,
        perfil: Perfil,
        setor: Setor? = null,
    ) = PrincipalUsuario(usuario.id, usuario.nome, usuario.email, perfil, usuario.provedor?.id, setor?.let { setOf(it.id) } ?: emptySet())

    private fun chamado(
        setor: Setor,
        atendente: Usuario?,
        status: StatusChamado,
    ) = Chamado(
        numero = "CH-TEST-1",
        titulo = "Teste",
        descricao = "Descrição de teste",
        setor = setor,
        categoria = Categoria(setor = setor),
        prioridade = Prioridade.MEDIA,
        status = status,
        solicitante = Usuario(nome = "Solicitante"),
        provedor = Provedor(nome = "Provedor"),
        atendente = atendente,
    )
}
