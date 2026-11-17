package br.com.centralchamados.migracao

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

@Testcontainers
class V2MigrationIntegrationTest {
    @Test
    fun `migration preserves retained category and deactivates referenced removed category`() {
        val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
        dataSource.setDriverClassName("org.postgresql.Driver")
        val flywayV1 =
            Flyway
                .configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1"))
                .load()
        flywayV1.migrate()

        val jdbc = JdbcTemplate(dataSource)
        val setorId = UUID.fromString("10000000-0000-0000-0000-000000000001")
        val provedorId =
            jdbc.queryForObject(
                "INSERT INTO provedor(nome) VALUES ('Migration test') RETURNING id",
                UUID::class.java,
            )!!
        val solicitanteId =
            jdbc.queryForObject(
                "INSERT INTO usuario(nome, email, senha_hash, perfil, provedor_id) VALUES ('Requester', 'migration@example.test', 'hash', 'SOLICITANTE', ?) RETURNING id",
                UUID::class.java,
                provedorId,
            )!!

        listOf("Equipamentos", "Rede interna").forEach { nome ->
            val categoriaId =
                jdbc.queryForObject(
                    "SELECT id FROM categoria WHERE setor_id = ? AND nome = ?",
                    UUID::class.java,
                    setorId,
                    nome,
                )!!
            jdbc.update(
                "INSERT INTO chamado(numero, titulo, descricao, setor_id, categoria_id, prioridade, status, solicitante_id, provedor_id) VALUES (?, 'Ticket', 'Description', ?, ?, 'MEDIA', 'ABERTO', ?, ?)",
                UUID.randomUUID().toString().take(20),
                setorId,
                categoriaId,
                solicitanteId,
                provedorId,
            )
        }

        Flyway
            .configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        val equipamentosAtivos =
            jdbc.queryForObject(
                "SELECT ativo FROM categoria WHERE setor_id = ? AND nome = 'Equipamentos'",
                Boolean::class.java,
                setorId,
            )
        val redeInternaAtiva =
            jdbc.queryForObject(
                "SELECT ativo FROM categoria WHERE setor_id = ? AND nome = 'Rede interna'",
                Boolean::class.java,
                setorId,
            )
        val equipamentosQuantidade =
            jdbc.queryForObject(
                "SELECT count(*) FROM categoria WHERE setor_id = ? AND nome = 'Equipamentos'",
                Long::class.javaObjectType,
                setorId,
            )

        assertTrue(equipamentosAtivos!!)
        assertEquals(false, redeInternaAtiva)
        assertEquals(1L, equipamentosQuantidade)
    }

    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
