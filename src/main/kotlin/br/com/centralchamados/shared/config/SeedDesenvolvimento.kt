package br.com.centralchamados.shared.config

import br.com.centralchamados.categorias.repository.CategoriaRepository
import br.com.centralchamados.chamados.model.Chamado
import br.com.centralchamados.chamados.repository.ChamadoRepository
import br.com.centralchamados.provedores.model.Provedor
import br.com.centralchamados.provedores.repository.ProvedorRepository
import br.com.centralchamados.setores.repository.SetorRepository
import br.com.centralchamados.shared.model.Perfil
import br.com.centralchamados.shared.model.Prioridade
import br.com.centralchamados.shared.model.StatusChamado
import br.com.centralchamados.usuarios.model.Usuario
import br.com.centralchamados.usuarios.repository.UsuarioRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Configuration
@Profile("dev")
class SeedDesenvolvimento {
    @Bean
    fun inicializarDados(
        usuarios: UsuarioRepository,
        provedores: ProvedorRepository,
        setores: SetorRepository,
        categorias: CategoriaRepository,
        chamados: ChamadoRepository,
        encoder: PasswordEncoder,
        jdbc: JdbcTemplate,
    ) = ApplicationRunner {
        if (usuarios.count() > 0) return@ApplicationRunner

        val listaSetores =
            (1..4).map {
                val id = UUID.fromString("10000000-0000-0000-0000-${it.toString().padStart(12, '0')}")
                setores.findById(id).orElseThrow()
            }
        val hardware = listaSetores[0]
        val desenvolvimento = listaSetores[1]
        val comercial = listaSetores[2]
        val operacional = listaSetores[3]
        val provedorA = provedores.save(Provedor(nome = "Provedor Aurora", cnpj = "12345678000190"))
        val provedorB = provedores.save(Provedor(nome = "Conecta Sul", cnpj = "98765432000110"))
        val admin =
            usuarios.save(
                Usuario(
                    nome = "Admin Central",
                    email = "admin@central.local",
                    senhaHash = encoder.encode("Senha123!"),
                    perfil = Perfil.ADMIN,
                ),
            )
        val atendentes =
            listOf(hardware, desenvolvimento, comercial, operacional).mapIndexed { indice, setor ->
                usuarios.save(
                    Usuario(
                        nome = "Atendente ${setor.nome}",
                        email = "atendente${indice + 1}@central.local",
                        senhaHash = encoder.encode("Senha123!"),
                        perfil = Perfil.ATENDENTE,
                        setores = mutableSetOf(setor),
                    ),
                )
            }
        val solicitantesA =
            (1..2).map { indice ->
                usuarios.save(
                    Usuario(
                        nome = "Solicitante Aurora $indice",
                        email = "solicitante$indice@aurora.local",
                        senhaHash = encoder.encode("Senha123!"),
                        perfil = Perfil.SOLICITANTE,
                        provedor = provedorA,
                    ),
                )
            }
        val solicitantesB =
            (1..2).map { indice ->
                usuarios.save(
                    Usuario(
                        nome = "Solicitante Conecta $indice",
                        email = "solicitante$indice@conecta.local",
                        senhaHash = encoder.encode("Senha123!"),
                        perfil = Perfil.SOLICITANTE,
                        provedor = provedorB,
                    ),
                )
            }
        val categoriaHardware = requireNotNull(categorias.findBySetorIdAndNomeAndAtivoTrue(hardware.id, "Equipamentos"))
        val categoriaDev = requireNotNull(categorias.findBySetorIdAndNomeAndAtivoTrue(desenvolvimento.id, "Correção de bugs"))
        val categoriaComercial = requireNotNull(categorias.findBySetorIdAndNomeAndAtivoTrue(comercial.id, "Criação de planos/combos"))
        val agora = Instant.now()

        fun numero(): String {
            val seq = jdbc.queryForObject("SELECT nextval('chamado_seq')", Long::class.javaObjectType)!!
            return "CH-${java.time.Year.now(java.time.ZoneOffset.UTC)}-${seq.toString().padStart(6, '0')}"
        }
        val chamadosSeed =
            listOf(
                Chamado(
                    numero = numero(),
                    titulo = "Roteador sem conexão",
                    descricao = "Equipamento reinicia ao conectar",
                    setor = hardware,
                    categoria = categoriaHardware,
                    prioridade = Prioridade.ALTA,
                    solicitante = solicitantesA[0],
                    provedor = provedorA,
                    criadoEm = agora.minus(2, ChronoUnit.HOURS),
                    atualizadoEm = agora.minus(2, ChronoUnit.HOURS),
                ),
                Chamado(
                    numero = numero(),
                    titulo = "Erro no painel",
                    descricao = "Falha ao carregar o painel",
                    setor = desenvolvimento,
                    categoria = categoriaDev,
                    prioridade = Prioridade.MEDIA,
                    status = StatusChamado.EM_ANDAMENTO,
                    solicitante = solicitantesA[1],
                    provedor = provedorA,
                    atendente = atendentes[1],
                    previsaoResolucao = agora.plus(2, ChronoUnit.HOURS),
                    assumidoEm = agora.minus(1, ChronoUnit.HOURS),
                    criadoEm = agora.minus(3, ChronoUnit.HOURS),
                    atualizadoEm = agora.minus(1, ChronoUnit.HOURS),
                ),
                Chamado(
                    numero = numero(),
                    titulo = "Atualizar catálogo",
                    descricao = "Incluir nova campanha comercial",
                    setor = comercial,
                    categoria = categoriaComercial,
                    prioridade = Prioridade.BAIXA,
                    status = StatusChamado.CONCLUIDO,
                    solicitante = solicitantesB[0],
                    provedor = provedorB,
                    atendente = atendentes[2],
                    previsaoResolucao = agora.minus(2, ChronoUnit.HOURS),
                    observacaoResolucao = "Catálogo atualizado e publicado.",
                    concluidoEm = agora.minus(2, ChronoUnit.HOURS),
                    assumidoEm = agora.minus(5, ChronoUnit.HOURS),
                    criadoEm = agora.minus(6, ChronoUnit.HOURS),
                    atualizadoEm = agora.minus(2, ChronoUnit.HOURS),
                ),
            )
        chamados.saveAll(chamadosSeed)
        jdbc.update(
            "INSERT INTO chamado_log(chamado_id, tipo_evento, autor_id, dados) VALUES (?, 'CRIADO', ?, ?::jsonb)",
            chamadosSeed[0].id,
            solicitantesA[0].id,
            """{"setorId":"${hardware.id}","provedorId":"${provedorA.id}"}""",
        )
        check(admin.ativo)
    }
}
