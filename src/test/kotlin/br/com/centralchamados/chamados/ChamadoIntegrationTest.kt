package br.com.centralchamados.chamados

import br.com.centralchamados.auditoria.AuditoriaService
import br.com.centralchamados.auth.JwtService
import br.com.centralchamados.auth.PrincipalUsuario
import br.com.centralchamados.categorias.model.Categoria
import br.com.centralchamados.categorias.repository.CategoriaRepository
import br.com.centralchamados.chamados.repository.ChamadoRepository
import br.com.centralchamados.notificacoes.AlertasAgendados
import br.com.centralchamados.notificacoes.EmailSender
import br.com.centralchamados.notificacoes.NotificacaoEnvioService
import br.com.centralchamados.notificacoes.NotificacaoService
import br.com.centralchamados.notificacoes.OutboxWorker
import br.com.centralchamados.notificacoes.PushSender
import br.com.centralchamados.notificacoes.ResultadoPush
import br.com.centralchamados.notificacoes.model.DispositivoPush
import br.com.centralchamados.notificacoes.model.PreferenciaNotificacao
import br.com.centralchamados.notificacoes.repository.DispositivoPushRepository
import br.com.centralchamados.notificacoes.repository.NotificacaoEnvioRepository
import br.com.centralchamados.notificacoes.repository.PreferenciaNotificacaoRepository
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
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataAccessException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(
    properties = ["app.outbox.interval-ms=86400000", "app.jwt.secret=test-jwt-secret-value-that-is-more-than-thirty-two-bytes"],
)
class ChamadoIntegrationTest {
    @SpykBean private lateinit var service: ChamadoService

    @Autowired private lateinit var auditoria: AuditoriaService

    @Autowired private lateinit var usuarios: UsuarioRepository

    @Autowired private lateinit var provedores: ProvedorRepository

    @Autowired private lateinit var setores: SetorRepository

    @Autowired private lateinit var categorias: CategoriaRepository

    @Autowired private lateinit var chamados: ChamadoRepository

    @Autowired private lateinit var jdbc: JdbcTemplate

    @Autowired private lateinit var alertas: AlertasAgendados

    @Autowired private lateinit var notificacoes: NotificacaoService

    @Autowired private lateinit var processamento: NotificacaoEnvioService

    @Autowired private lateinit var outbox: OutboxWorker

    @Autowired private lateinit var envios: NotificacaoEnvioRepository

    @Autowired private lateinit var preferencias: PreferenciaNotificacaoRepository

    @Autowired private lateinit var dispositivos: DispositivoPushRepository

    @Autowired private lateinit var jwt: JwtService

    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var pushSender: PushSender

    @MockkBean(relaxed = true)
    private lateinit var emailSender: EmailSender

    @Test
    fun `isola solicitantes e atendentes por consulta de banco`() {
        val fixture = criarFixture()
        val solicitanteMesmoProvedor = criarSolicitante("colega", fixture.provedor)
        val outroSolicitante = criarSolicitante("isolado", provedores.save(Provedor(nome = "Outro provedor")))
        val ticket = abrir(fixture)
        val atendenteOutroSetor = criarAtendente("fora", Setor(nome = "Setor alheio").let(setores::save))

        assertEquals(1, service.listar(principal(fixture.solicitante), ChamadoFiltros(), PageRequest.of(0, 20)).totalElements)
        val erroSolicitante = assertThrows(ErroApi::class.java) { service.obter(principal(outroSolicitante), ticket.id) }
        assertEquals("CHAMADO_NAO_ENCONTRADO", erroSolicitante.codigo)
        val erroMesmoProvedor = assertThrows(ErroApi::class.java) { service.obter(principal(solicitanteMesmoProvedor), ticket.id) }
        assertEquals("CHAMADO_NAO_ENCONTRADO", erroMesmoProvedor.codigo)
        val erroAtendente = assertThrows(ErroApi::class.java) { service.obter(principal(atendenteOutroSetor), ticket.id) }
        assertEquals("CHAMADO_NAO_ENCONTRADO", erroAtendente.codigo)
    }

    @Test
    fun `concorrencia de assumir produz um unico atendente e conflito`() {
        val fixture = criarFixture()
        val outroAtendente = criarAtendente("segundo", fixture.setor)
        val ticket = abrir(fixture)
        val partida = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val previsao = Instant.now().plusSeconds(3600)
        val resultados =
            listOf(fixture.atendente, outroAtendente).map { atendente ->
                pool.submit<Any> {
                    partida.await()
                    runCatching { service.assumir(principal(atendente), ticket.id, AssumirChamadoRequest(previsao)) }.fold({ it }, { it })
                }
            }
        partida.countDown()
        val respostas = resultados.map { it.get() }
        pool.shutdownNow()

        assertEquals(1, respostas.count { it is ChamadoResposta && it.status == StatusChamado.EM_ANDAMENTO })
        val falhas = respostas.filterIsInstance<Throwable>()
        assertEquals(1, falhas.size)
        val falha = falhas.single()
        assertEquals(
            org.springframework.http.HttpStatus.CONFLICT,
            when (falha) {
                is ErroApi -> falha.status
                is OptimisticLockingFailureException -> org.springframework.http.HttpStatus.CONFLICT
                else -> throw AssertionError("Falha inesperada na concorrência", falha)
            },
        )
    }

    @Test
    fun `logs sao imutaveis e notificacoes vao aos destinatarios corretos`() {
        val fixture = criarFixture()
        val ticket = abrir(fixture)
        val notificacoesAtendentes =
            jdbc.queryForObject(
                "SELECT count(*) FROM notificacao WHERE chamado_id = ? AND tipo_evento = 'CHAMADO_CRIADO'",
                Long::class.javaObjectType,
                ticket.id,
            )
        val notificacoesSolicitante =
            jdbc.queryForObject(
                "SELECT count(*) FROM notificacao WHERE chamado_id = ? AND destinatario_id = ?",
                Long::class.javaObjectType,
                ticket.id,
                fixture.solicitante.id,
            )
        val logs = auditoria.listarDoChamado(principal(fixture.atendente), ticket.id)

        assertEquals(2L, notificacoesAtendentes)
        assertEquals(0L, notificacoesSolicitante)
        assertEquals("CRIADO", logs.single().tipoEvento)
        assertThrows(DataAccessException::class.java) {
            jdbc.update("UPDATE chamado_log SET dados = '{}'::jsonb WHERE chamado_id = ?", ticket.id)
        }
        assertThrows(DataAccessException::class.java) {
            jdbc.update("DELETE FROM chamado_log WHERE chamado_id = ?", ticket.id)
        }
    }

    @Test
    fun `alerta agendado nao duplica chamado e evento`() {
        val fixture = criarFixture()
        val ticket = abrir(fixture)
        service.assumir(principal(fixture.atendente), ticket.id, AssumirChamadoRequest(Instant.now().plusSeconds(1800)))

        alertas.verificar()
        alertas.verificar()

        val quantidade =
            jdbc.queryForObject(
                "SELECT count(*) FROM notificacao WHERE chamado_id = ? AND tipo_evento = 'PREVISAO_PROXIMA_VENCIMENTO' AND alerta_agendado",
                Long::class.javaObjectType,
                ticket.id,
            )
        assertEquals(1L, quantidade)
    }

    @Test
    fun `fluxo de estados validos registra auditoria ate a conclusao`() {
        val fixture = criarFixture()
        val aberto = abrir(fixture)
        val assumido = service.assumir(principal(fixture.atendente), aberto.id, AssumirChamadoRequest(Instant.now().plusSeconds(3600)))
        val prioridade =
            service.alterarPrioridade(
                principal(fixture.atendente),
                aberto.id,
                AlterarPrioridadeRequest(Prioridade.ALTA, "Impacto operacional"),
            )
        val concluido =
            service.concluir(
                principal(fixture.atendente),
                aberto.id,
                ConcluirChamadoRequest("Equipamento substituído e validado."),
            )

        assertEquals(StatusChamado.EM_ANDAMENTO, assumido.status)
        assertEquals(Prioridade.ALTA, prioridade.prioridade)
        assertEquals(StatusChamado.CONCLUIDO, concluido.status)
        assertNotNull(concluido.concluidoEm)
        assertEquals(
            listOf(
                "CRIADO",
                "ASSUMIDO",
                "STATUS_ALTERADO",
                "PRIORIDADE_ALTERADA",
                "CONCLUIDO",
                "STATUS_ALTERADO",
            ),
            auditoria.listarDoChamado(principal(fixture.atendente), aberto.id).map {
                it.tipoEvento
            },
        )
    }

    @Test
    fun `chamado critico notifica atendentes e admin mas nao autor`() {
        val fixture = criarFixture()
        criarAdmin()
        val ticket = abrir(fixture, Prioridade.CRITICA)

        val destinatarios =
            jdbc
                .queryForList(
                    "SELECT destinatario_id FROM notificacao WHERE chamado_id = ? AND tipo_evento = 'CHAMADO_CRITICO'",
                    UUID::class.java,
                    ticket.id,
                ).toSet()

        val admins = usuarios.findByPerfilAndAtivo(Perfil.ADMIN).map { it.id }.toSet()
        assertEquals(setOf(fixture.atendente.id, fixture.outroAtendente.id) + admins, destinatarios)
        assertEquals(false, ticket.solicitanteId in destinatarios)
    }

    @Test
    fun `alerta de previsao vencida notifica responsavel e admin`() {
        val fixture = criarFixture()
        criarAdmin()
        val ticket = abrir(fixture)
        service.assumir(principal(fixture.atendente), ticket.id, AssumirChamadoRequest(Instant.now().plusSeconds(3600)))
        jdbc.update("UPDATE chamado SET previsao_resolucao = now() - interval '1 hour' WHERE id = ?", ticket.id)

        alertas.verificar()

        val destinatarios =
            jdbc
                .queryForList(
                    "SELECT destinatario_id FROM notificacao WHERE chamado_id = ? AND tipo_evento = 'PREVISAO_VENCIDA'",
                    UUID::class.java,
                    ticket.id,
                ).toSet()
        val admins = usuarios.findByPerfilAndAtivo(Perfil.ADMIN).map { it.id }.toSet()
        assertEquals(setOf(fixture.atendente.id) + admins, destinatarios)
    }

    @Test
    fun `outbox marca envio como enviado no sucesso`() {
        val fixture = criarFixture()
        val ticket = abrir(fixture)
        outbox.processar()

        val statusEnvio =
            jdbc.queryForObject(
                "SELECT e.status FROM notificacao_envio e JOIN notificacao n ON n.id = e.notificacao_id WHERE n.chamado_id = ? AND e.canal = 'EMAIL' LIMIT 1",
                String::class.java,
                ticket.id,
            )
        assertEquals("ENVIADO", statusEnvio)
    }

    @Test
    fun `outbox marca envio como falhou apos tres erros`() {
        val fixture = criarFixture()
        val ticket = abrir(fixture)
        dispositivos.save(DispositivoPush(usuario = fixture.atendente, token = "failure-${UUID.randomUUID()}", plataforma = "ios"))
        val envioId =
            jdbc.queryForObject(
                "SELECT e.id FROM notificacao_envio e JOIN notificacao n ON n.id = e.notificacao_id WHERE n.chamado_id = ? AND n.destinatario_id = ? AND e.canal = 'PUSH' LIMIT 1",
                UUID::class.java,
                ticket.id,
                fixture.atendente.id,
            )!!
        every { pushSender.enviar(any(), any(), any(), any()) } throws IllegalStateException("push indisponível")

        repeat(3) { processamento.processar(envioId) }

        assertEquals("FALHOU", jdbc.queryForObject("SELECT status FROM notificacao_envio WHERE id = ?", String::class.java, envioId))
        assertEquals(3, jdbc.queryForObject("SELECT tentativas FROM notificacao_envio WHERE id = ?", Int::class.javaObjectType, envioId))
    }

    @Test
    fun `preferencia push desativada mantem notificacao local sem envio push`() {
        val fixture = criarFixture()
        val ticket = abrir(fixture)
        preferencias.save(
            PreferenciaNotificacao(
                fixture.atendente.id,
                "PUSH_DESATIVADO",
                br.com.centralchamados.shared.model.CanalNotificacao.PUSH,
                false,
            ),
        )

        notificacoes.notificar(
            fixture.atendente,
            chamados.findById(ticket.id).orElseThrow(),
            "PUSH_DESATIVADO",
            "Título",
            "Mensagem",
        )

        assertEquals(
            1L,
            jdbc.queryForObject(
                "SELECT count(*) FROM notificacao WHERE chamado_id = ? AND destinatario_id = ? AND tipo_evento = 'PUSH_DESATIVADO'",
                Long::class.javaObjectType,
                ticket.id,
                fixture.atendente.id,
            ),
        )
        assertEquals(
            0L,
            jdbc.queryForObject(
                "SELECT count(*) FROM notificacao_envio e JOIN notificacao n ON n.id = e.notificacao_id WHERE n.chamado_id = ? AND n.destinatario_id = ? AND n.tipo_evento = 'PUSH_DESATIVADO' AND e.canal = 'PUSH'",
                Long::class.javaObjectType,
                ticket.id,
                fixture.atendente.id,
            ),
        )
    }

    @Test
    fun `push invalido desativa dispositivo`() {
        val fixture = criarFixture()
        val ticket = abrir(fixture)
        val device =
            dispositivos.save(
                DispositivoPush(usuario = fixture.atendente, token = "invalid-${UUID.randomUUID()}", plataforma = "android"),
            )
        val envioId =
            jdbc.queryForObject(
                "SELECT e.id FROM notificacao_envio e JOIN notificacao n ON n.id = e.notificacao_id WHERE n.chamado_id = ? AND n.destinatario_id = ? AND e.canal = 'PUSH' LIMIT 1",
                UUID::class.java,
                ticket.id,
                fixture.atendente.id,
            )!!
        every { pushSender.enviar(any(), any(), any(), any()) } returns ResultadoPush(true, true)

        processamento.processar(envioId)

        assertEquals(false, dispositivos.findById(device.id).orElseThrow().ativo)
    }

    @Test
    fun `mockmvc exige token para endpoints protegidos`() {
        mockMvc.perform(get("/api/v1/chamados")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `mockmvc oculta chamado de outro solicitante como problem json`() {
        val fixture = criarFixture()
        val outroSolicitante = criarSolicitante("outro", fixture.provedor)
        val ticket = abrir(fixture)

        mockMvc
            .perform(
                get("/api/v1/chamados/${ticket.id}")
                    .header("Authorization", "Bearer ${jwt.accessToken(outroSolicitante)}"),
            ).andExpect(status().isNotFound)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.codigo").value("CHAMADO_NAO_ENCONTRADO"))
    }

    @Test
    fun `mockmvc traduz optimistic locking para conflito`() {
        val fixture = criarFixture()
        val ticket = abrir(fixture)
        every { service.assumir(any(), eq(ticket.id), any()) } throws
            OptimisticLockingFailureException("Chamado alterado")

        mockMvc
            .perform(
                post("/api/v1/chamados/${ticket.id}/assumir")
                    .header("Authorization", "Bearer ${jwt.accessToken(fixture.atendente)}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"previsaoResolucao":"${Instant.now().plusSeconds(3600)}"}"""),
            ).andExpect(status().isConflict)
    }

    private fun criarFixture(): Fixture {
        val setor = setores.save(Setor(nome = "Teste ${UUID.randomUUID()}"))
        val categoria = categorias.save(Categoria(setor = setor, nome = "Categoria"))
        val provedor = provedores.save(Provedor(nome = "Provedor ${UUID.randomUUID()}"))
        val solicitante = criarSolicitante("solicitante", provedor)
        val atendente = criarAtendente("atendente", setor)
        val outroAtendente = criarAtendente("colega", setor)
        return Fixture(setor, categoria, provedor, solicitante, atendente, outroAtendente)
    }

    private fun criarSolicitante(
        prefixo: String,
        provedor: Provedor,
    ) = usuarios.save(
        Usuario(nome = prefixo, email = "$prefixo-${UUID.randomUUID()}@example.test", perfil = Perfil.SOLICITANTE, provedor = provedor),
    )

    private fun criarAtendente(
        prefixo: String,
        setor: Setor,
    ) = usuarios.save(
        Usuario(
            nome = prefixo,
            email = "$prefixo-${UUID.randomUUID()}@example.test",
            perfil = Perfil.ATENDENTE,
            setores = mutableSetOf(setor),
        ),
    )

    private fun criarAdmin() =
        usuarios.save(
            Usuario(nome = "Admin", email = "admin-${UUID.randomUUID()}@example.test", perfil = Perfil.ADMIN),
        )

    private fun abrir(
        fixture: Fixture,
        prioridade: Prioridade = Prioridade.MEDIA,
    ) = service.abrir(
        principal(fixture.solicitante),
        AbrirChamadoRequest(fixture.setor.id, fixture.categoria.id, prioridade, "Título", "Descrição do chamado"),
    )

    private fun principal(usuario: Usuario) =
        PrincipalUsuario(
            usuario.id,
            usuario.nome,
            usuario.email,
            usuario.perfil,
            usuario.provedor?.id,
            usuario.setores.map { it.id }.toSet(),
        )

    private data class Fixture(
        val setor: Setor,
        val categoria: Categoria,
        val provedor: Provedor,
        val solicitante: Usuario,
        val atendente: Usuario,
        val outroAtendente: Usuario,
    )

    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer("postgres:16")

        @JvmStatic
        @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
